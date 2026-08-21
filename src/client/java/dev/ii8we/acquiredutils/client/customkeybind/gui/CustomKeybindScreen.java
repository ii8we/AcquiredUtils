package dev.ii8we.acquiredutils.client.customkeybind.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ii8we.acquiredutils.client.customkeybind.CustomKeybind;
import dev.ii8we.acquiredutils.client.customkeybind.CustomKeybindManager;
import dev.ii8we.acquiredutils.client.gui.theme.Theme;
import dev.ii8we.acquiredutils.client.gui.widget.ThemedButtonWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class CustomKeybindScreen extends Screen {
    private final Screen parent;
    private static final int PANEL_WIDTH = 600;
    private static final int PANEL_HEIGHT = 330;
    private static final int ROW_HEIGHT = 64;
    private static final int ROW_GAP = 8;

    private int panelX;
    private int panelY;
    private int scroll;
    private int maxScroll;
    private final List<RowBounds> rows = new ArrayList<>();
    private record RowBounds(String id, int x, int y, int w, int h) {}

    public CustomKeybindScreen(Screen parent) {
        super(Component.literal("Custom Keybinds"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        rows.clear();

        int width = Math.min(PANEL_WIDTH, this.width - 30);
        int height = Math.min(PANEL_HEIGHT, this.height - 30);
        panelX = (this.width - width) / 2;
        panelY = (this.height - height) / 2;

        addRenderableWidget(new ThemedButtonWidget(
            panelX + width - 114, panelY + 18, 92, 22,
            Component.literal("+ Add"),
            () -> minecraft.setScreen(new CustomKeybindEditorScreen(this, null))
        ));

        addRenderableWidget(new ThemedButtonWidget(
            panelX + 14, panelY + height - 34, 92, 22,
            Component.literal("Back"),
            this::onClose
        ));

        int viewportTop = panelY + 56;
        int viewportBottom = panelY + height - 44;
        int contentHeight = CustomKeybindManager.getAll().size() * (ROW_HEIGHT + ROW_GAP);
        maxScroll = Math.max(0, contentHeight - (viewportBottom - viewportTop));
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        int rowY = viewportTop - scroll;
        for (CustomKeybind keybind : CustomKeybindManager.getAll()) {
            int x = panelX + 14;
            int rowW = width - 28;
            rows.add(new RowBounds(keybind.getId(), x, rowY, rowW, ROW_HEIGHT));

            if (rowY + ROW_HEIGHT >= viewportTop && rowY <= viewportBottom) {
                addRenderableWidget(new ThemedButtonWidget(
                    x + rowW - 158, rowY + 34, 70, 20,
                    Component.literal("Edit"),
                    () -> minecraft.setScreen(new CustomKeybindEditorScreen(this, keybind))
                ));
                addRenderableWidget(new ThemedButtonWidget(
                    x + rowW - 82, rowY + 34, 70, 20,
                    Component.literal("Delete"),
                    () -> {
                        CustomKeybindManager.remove(keybind.getId());
                        init();
                    }
                ));
            }
            rowY += ROW_HEIGHT + ROW_GAP;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int width = Math.min(PANEL_WIDTH, this.width - 30);
        int height = Math.min(PANEL_HEIGHT, this.height - 30);
        if (mouseX >= panelX && mouseX <= panelX + width
            && mouseY >= panelY + 50 && mouseY <= panelY + height - 40) {
            int old = scroll;
            scroll -= (int) (scrollY * 30);
            scroll = Math.max(0, Math.min(scroll, maxScroll));
            if (old != scroll) {
                init();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Theme theme = Theme.current();
        graphics.fill(0, 0, width, height, 0x8A05030A);

        int widthValue = Math.min(PANEL_WIDTH, this.width - 30);
        int heightValue = Math.min(PANEL_HEIGHT, this.height - 30);
        int right = panelX + widthValue;
        int bottom = panelY + heightValue;

        graphics.fillGradient(panelX, panelY, right, bottom,
            theme.sidebarTop, theme.sidebarBottom);
        graphics.renderOutline(panelX, panelY, widthValue, heightValue, theme.frameMid);

        graphics.drawString(font, getTitle(), panelX + 14, panelY + 22, theme.text, false);
        graphics.drawString(font,
            Component.literal("Create a key that sends a message or command."),
            panelX + 14, panelY + 39, theme.credit, false);

        int viewportTop = panelY + 56;
        int viewportBottom = bottom - 44;
        graphics.enableScissor(panelX + 8, viewportTop, right - 8, viewportBottom);

        for (CustomKeybind keybind : CustomKeybindManager.getAll()) {
            RowBounds row = rows.stream()
                .filter(bounds -> bounds.id().equals(keybind.getId()))
                .findFirst()
                .orElse(null);
            if (row == null) {
                continue;
            }
            drawRow(graphics, theme, row, keybind);
        }

        graphics.disableScissor();

        if (CustomKeybindManager.getAll().isEmpty()) {
            int textWidth = font.width("No custom keybinds yet.");
            graphics.drawString(font, "No custom keybinds yet.",
                panelX + (widthValue - textWidth) / 2,
                panelY + heightValue / 2,
                theme.credit, false);
        }

        if (maxScroll > 0) {
            int trackX = right - 8;
            int trackTop = viewportTop;
            int trackBottom = viewportBottom;
            graphics.fill(trackX, trackTop, trackX + 3, trackBottom, theme.footerBottom);
            int thumbHeight = Math.max(20, (trackBottom - trackTop) * (trackBottom - trackTop)
                / Math.max(1, trackBottom - trackTop + maxScroll));
            int thumbY = trackTop + (trackBottom - trackTop - thumbHeight) * scroll / Math.max(1, maxScroll);
            graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, theme.accent);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawRow(GuiGraphics graphics, Theme theme, RowBounds row, CustomKeybind keybind) {
        int x = row.x();
        int y = row.y();
        int w = row.w();
        int h = row.h();

        graphics.fillGradient(x, y, x + w, y + h,
            theme.headerTop, theme.headerBottom);
        graphics.renderOutline(x, y, w, h, theme.frameMid);

        graphics.drawString(font, Component.literal(keybind.getName()),
            x + 12, y + 8, theme.text, false);
        graphics.drawString(font,
            Component.literal(keybind.getActionText()),
            x + 12, y + 27,
            theme.credit, false);

        String keyText = formatKeybind(keybind);
        int keyWidth = font.width(keyText);
        graphics.drawString(font, keyText,
            x + w - keyWidth - 14, y + 8, theme.accentBright, false);
    }

    private static String formatKeybind(CustomKeybind keybind) {
        if (keybind.getKeyCode() < 0) {
            return "NONE";
        }

        String keyName = InputConstants.Type.KEYSYM.getOrCreate(keybind.getKeyCode())
            .getDisplayName().getString();
        StringBuilder result = new StringBuilder();
        if (keybind.isControl()) {
            result.append("CTRL + ");
        }
        if (keybind.isAlt()) {
            result.append("ALT + ");
        }
        if (keybind.isShift()) {
            result.append("SHIFT + ");
        }
        return result.append(keyName).toString();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
