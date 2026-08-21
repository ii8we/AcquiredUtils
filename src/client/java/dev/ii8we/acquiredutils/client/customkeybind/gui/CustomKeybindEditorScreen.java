package dev.ii8we.acquiredutils.client.customkeybind.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ii8we.acquiredutils.client.customkeybind.CustomKeybind;
import dev.ii8we.acquiredutils.client.customkeybind.CustomKeybindManager;
import dev.ii8we.acquiredutils.client.gui.theme.Theme;
import dev.ii8we.acquiredutils.client.gui.widget.ThemedButtonWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class CustomKeybindEditorScreen extends Screen {
    private final Screen parent;
    private final CustomKeybind editing;

    private EditBox nameBox;
    private EditBox actionBox;
    private int capturedKey = -1;
    private boolean capturedControl;
    private boolean capturedAlt;
    private boolean capturedShift;
    private boolean listening;
    private ThemedButtonWidget keyButton;
    private Component errorMessage = Component.empty();

    public CustomKeybindEditorScreen(Screen parent, CustomKeybind editing) {
        super(Component.literal(editing == null ? "Create Custom Keybind" : "Edit Custom Keybind"));
        this.parent = parent;
        this.editing = editing == null ? null : editing.copy();
        if (this.editing != null) {
            this.capturedKey = this.editing.getKeyCode();
            this.capturedControl = this.editing.isControl();
            this.capturedAlt = this.editing.isAlt();
            this.capturedShift = this.editing.isShift();
        }
    }

    @Override
    protected void init() {
        clearWidgets();

        int panelWidth = Math.min(540, width - 40);
        int panelHeight = 220;
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;

        nameBox = new EditBox(font, left + 22, top + 58, panelWidth - 44, 20, Component.literal("Name"));
        nameBox.setMaxLength(64);
        nameBox.setValue(editing == null ? "" : editing.getName());
        addRenderableWidget(nameBox);

        keyButton = new ThemedButtonWidget(
            left + 22, top + 92, panelWidth - 44, 22,
            Component.literal(displayKey()),
            () -> {
                listening = !listening;
                errorMessage = Component.empty();
                updateKeyCaptureVisual();
            }
        );
        keyButton.setHighlighted(listening);
        addRenderableWidget(keyButton);

        actionBox = new EditBox(font, left + 22, top + 134, panelWidth - 44, 20, Component.literal("Message or command"));
        actionBox.setMaxLength(256);
        actionBox.setValue(editing == null ? "" : editing.getActionText());
        addRenderableWidget(actionBox);

        addRenderableWidget(new ThemedButtonWidget(
            left + panelWidth - 172, top + panelHeight - 34, 150, 22,
            Component.literal("Save"),
            this::save
        ));
        addRenderableWidget(new ThemedButtonWidget(
            left + 22, top + panelHeight - 34, 120, 22,
            Component.literal("Cancel"),
            this::onClose
        ));
    }

    private String displayKey() {
        if (listening) {
            return "Press a key...";
        }
        if (capturedKey < 0) {
            return "NONE";
        }

        String keyName = InputConstants.Type.KEYSYM.getOrCreate(capturedKey).getDisplayName().getString();
        StringBuilder result = new StringBuilder();
        if (capturedControl) {
            result.append("CTRL + ");
        }
        if (capturedAlt) {
            result.append("ALT + ");
        }
        if (capturedShift) {
            result.append("SHIFT + ");
        }
        return result.append(keyName).toString();
    }

    private void updateKeyCaptureVisual() {
        if (keyButton == null) {
            return;
        }
        keyButton.setHighlighted(listening);
        keyButton.setMessage(Component.literal(displayKey()));
    }

    private void save() {
        String name = nameBox.getValue().trim();
        String text = actionBox.getValue();
        if (name.isBlank()) {
            errorMessage = Component.literal("Name cannot be empty.");
            return;
        }
        if (text.isBlank()) {
            errorMessage = Component.literal("Message or command cannot be empty.");
            return;
        }
        if (capturedKey >= 0 && CustomKeybindManager.isReservedBinding(
            capturedKey, capturedControl, capturedAlt, capturedShift, editing == null ? null : editing.getId())) {
            errorMessage = Component.literal("That key combination is already in use.");
            return;
        }

        boolean success;
        if (editing == null) {
            try {
                CustomKeybindManager.create(name, capturedKey, capturedControl, capturedAlt, capturedShift, text);
                success = true;
            } catch (IllegalArgumentException e) {
                errorMessage = Component.literal(e.getMessage());
                return;
            }
        } else {
            CustomKeybind updated = new CustomKeybind(
                editing.getId(), name, capturedKey, capturedControl, capturedAlt, capturedShift, text);
            success = CustomKeybindManager.update(updated);
            if (!success) {
                errorMessage = Component.literal("That key combination is already in use.");
                return;
            }
        }

        if (success) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listening) {
            int keyCode = event.key();
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listening = false;
                errorMessage = Component.empty();
                updateKeyCaptureVisual();
                return true;
            }

            if (isModifierKey(keyCode)) {
                return true;
            }

            if (keyCode >= 0) {
                boolean control = event.hasControlDown();
                boolean alt = event.hasAltDown();
                boolean shift = event.hasShiftDown();
                if (CustomKeybindManager.isReservedBinding(
                    keyCode, control, alt, shift, editing == null ? null : editing.getId())) {
                    errorMessage = Component.literal("That key combination is already in use.");
                    return true;
                }
                capturedKey = keyCode;
                capturedControl = control;
                capturedAlt = alt;
                capturedShift = shift;
                listening = false;
                errorMessage = Component.empty();
                updateKeyCaptureVisual();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    private static boolean isModifierKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_LEFT_CONTROL
            || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL
            || keyCode == GLFW.GLFW_KEY_LEFT_ALT
            || keyCode == GLFW.GLFW_KEY_RIGHT_ALT
            || keyCode == GLFW.GLFW_KEY_LEFT_SHIFT
            || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Theme theme = Theme.current();
        graphics.fill(0, 0, width, height, 0x8A05030A);

        int panelWidth = Math.min(540, width - 40);
        int panelHeight = 220;
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;

        graphics.fillGradient(left, top, left + panelWidth, top + panelHeight,
            theme.sidebarTop, theme.sidebarBottom);
        graphics.renderOutline(left, top, panelWidth, panelHeight, theme.frameMid);
        graphics.drawString(font, getTitle(), left + 22, top + 20, theme.text, false);

        graphics.drawString(font, Component.literal("Name"), left + 22, top + 45, theme.text, false);
        graphics.drawString(font, Component.literal("Message or command"), left + 22, top + 121, theme.text, false);

        if (!errorMessage.getString().isEmpty()) {
            graphics.drawString(font, errorMessage, left + 22, top + 162, theme.accentBright, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
