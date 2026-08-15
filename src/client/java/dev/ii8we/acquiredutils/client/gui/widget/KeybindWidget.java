package dev.ii8we.acquiredutils.client.gui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ii8we.acquiredutils.client.gui.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class KeybindWidget extends AbstractWidget implements KeyListenerSlot.Listener {
    private static final int KEY_BOX_WIDTH = 76;
    private static final int KEY_BOX_OFFSET_LEFT = 18;
    private final KeyListenerSlot slot;
    private final IntSupplier keyGetter;
    private final IntConsumer keySetter;

    public KeybindWidget(
        int x,
        int y,
        int width,
        int height,
        KeyListenerSlot slot,
        IntSupplier keyGetter,
        IntConsumer keySetter
    ) {
        super(x, y, width, height, Component.empty());
        this.slot = slot;
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
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Theme theme = Theme.current();
        var font = Minecraft.getInstance().font;
        boolean listening = slot.isListening(this);
        int keyBoxX = getX() + width - KEY_BOX_WIDTH - KEY_BOX_OFFSET_LEFT;
        String keyText = listening
            ? "..."
            : (keyGetter.getAsInt() < 0
                ? "[NONE]"
                : InputConstants.Type.KEYSYM.getOrCreate(keyGetter.getAsInt()).getDisplayName().getString());
        int keyColor = listening ? theme.accentBright : (keyGetter.getAsInt() < 0 ? theme.credit : theme.text);

        graphics.fill(keyBoxX, getY(), keyBoxX + KEY_BOX_WIDTH, getY() + height, theme.footerBottom);
        graphics.renderOutline(
            keyBoxX, getY(), KEY_BOX_WIDTH, height,
            listening ? theme.accentBright : theme.frameMid
        );

        int textWidth = font.width(keyText);
        graphics.drawString(
            font,
            keyText,
            keyBoxX + (KEY_BOX_WIDTH - textWidth) / 2,
            getY() + (height - 8) / 2,
            keyColor,
            false
        );
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        int keyBoxX = getX() + width - KEY_BOX_WIDTH - KEY_BOX_OFFSET_LEFT;
        if (event.x() >= keyBoxX
            && event.x() < keyBoxX + KEY_BOX_WIDTH
            && event.y() >= getY()
            && event.y() < getY() + height) {
            slot.current = this;
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.translatable("acquiredutils.gui.keybind.slot_lock"));
    }
}
