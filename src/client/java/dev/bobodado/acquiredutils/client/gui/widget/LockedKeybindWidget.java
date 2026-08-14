package dev.bobodado.acquiredutils.client.gui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import dev.bobodado.acquiredutils.client.gui.theme.Theme;
import dev.bobodado.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class LockedKeybindWidget extends AbstractWidget implements KeyListenerSlot.Listener {

    private static final int CHECKBOX_SIZE = 12;
    private static final int KEY_BOX_WIDTH = 70;
    private static final int KEY_BOX_RIGHT_GAP = 20;
    private static final int CONTENT_LEFT_INSET = 7;

    private static final Identifier CHECKBOX_CHECKED =
        Identifier.fromNamespaceAndPath(
            "acquiredutils",
            "textures/gui/checkbox_purple_checked.png"
        );

    private static final Identifier CHECKBOX_UNCHECKED =
        Identifier.fromNamespaceAndPath(
            "acquiredutils",
            "textures/gui/checkbox_purple_unchecked.png"
        );
    
    private final KeyListenerSlot slot;
    private final BooleanSupplier enabledGetter;
    private final Consumer<Boolean> enabledSetter;
    private final IntSupplier keyGetter;
    private final Consumer<Integer> keySetter;

    public LockedKeybindWidget(
        int x,
        int y,
        int width,
        int height,
        Component label,
        KeyListenerSlot slot,
        BooleanSupplier enabledGetter,
        Consumer<Boolean> enabledSetter,
        IntSupplier keyGetter,
        Consumer<Integer> keySetter
    ) {
        super(x, y, width, height, label);
        this.slot = slot;
        this.enabledGetter = enabledGetter;
        this.enabledSetter = enabledSetter;
        this.keyGetter = keyGetter;
        this.keySetter = keySetter;
    }

    @Override
    public void applyKeyCode(int keyCode) {
        keySetter.accept(keyCode);

        if (slot.isListening(this)) {
            slot.clear();
        }
    }

    @Override
    protected void renderWidget(
        GuiGraphics graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        Theme theme = Theme.current();
        var font = Minecraft.getInstance().font;

        boolean enabled = enabledGetter.getAsBoolean();
        boolean listening = slot.isListening(this);

        int cbSize = CHECKBOX_SIZE;
        int cbY = getY() + (height - cbSize) / 2;

        Identifier checkboxTexture = enabled
            ? CHECKBOX_CHECKED
            : CHECKBOX_UNCHECKED;

        int cbX = getX() + CONTENT_LEFT_INSET;

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            checkboxTexture,
            cbX,
            cbY,
            0.0f,
            0.0f,
            CHECKBOX_SIZE,
            CHECKBOX_SIZE,
            CHECKBOX_SIZE,
            CHECKBOX_SIZE
        );

        if (listening) {
            graphics.renderOutline(
                cbX - 1,
                cbY - 1,
                cbSize + 2,
                cbSize + 2,
                theme.accentBright
            );
        }

        int labelX = cbX + cbSize + 9;
        int labelY = getY() + (height - 8) / 2;

        graphics.drawString(
            font,
            getMessage(),
            labelX,
            labelY,
            theme.text,
            false
        );

        int keyBoxW = KEY_BOX_WIDTH;
        int keyBoxX = getX() + width - keyBoxW - KEY_BOX_RIGHT_GAP;
        int keyCode = keyGetter.getAsInt();

        String keyText = listening
            ? "..."
            : (keyCode < 0
                ? "[NONE]"
                : InputConstants.Type.KEYSYM
                    .getOrCreate(keyCode)
                    .getDisplayName()
                    .getString());

        int keyColor = listening
            ? theme.accentBright
            : (keyCode < 0 ? theme.credit : theme.text);

        graphics.fill(
            keyBoxX,
            getY(),
            keyBoxX + keyBoxW,
            getY() + height,
            theme.footerBottom
        );

        graphics.renderOutline(
            keyBoxX,
            getY(),
            keyBoxW,
            height,
            listening ? theme.accentBright : theme.frameMid
        );

        int textWidth = font.width(keyText);

        graphics.drawString(
            font,
            keyText,
            keyBoxX + (keyBoxW - textWidth) / 2,
            getY() + (height - 8) / 2,
            keyColor,
            false
        );
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        int cbSize = CHECKBOX_SIZE;
        int cbY = getY() + (height - cbSize) / 2;

        int cbX = getX() + CONTENT_LEFT_INSET;

        boolean onCheckbox =
            mouseX >= cbX
            && mouseX < cbX + cbSize
            && mouseY >= cbY
            && mouseY < cbY + cbSize;

        if (onCheckbox) {
            enabledSetter.accept(!enabledGetter.getAsBoolean());
            return;
        }

        int keyBoxW = KEY_BOX_WIDTH;
        int keyBoxX = getX() + width - keyBoxW - KEY_BOX_RIGHT_GAP;

        boolean onKeyBox =
            mouseX >= keyBoxX
            && mouseX < keyBoxX + keyBoxW
            && mouseY >= getY()
            && mouseY < getY() + height;

        if (onKeyBox) {
            slot.current = this;
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
