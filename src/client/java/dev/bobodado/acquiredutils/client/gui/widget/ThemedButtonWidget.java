package dev.bobodado.acquiredutils.client.gui.widget;

import dev.bobodado.acquiredutils.client.gui.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class ThemedButtonWidget extends AbstractWidget {

    private final Runnable clickHandler;
    private final boolean bold;

    public ThemedButtonWidget(int x, int y, int width, int height, Component label, Runnable clickHandler) {
        this(x, y, width, height, label, clickHandler, false);
    }

    public ThemedButtonWidget(int x, int y, int width, int height, Component label, Runnable clickHandler, boolean bold) {
        super(x, y, width, height, label);
        this.clickHandler = clickHandler;
        this.bold = bold;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Theme theme = Theme.current();
        boolean hovered = isHovered();
        boolean pressed = isFocused();

        int outer = hovered ? theme.accentBright : theme.frameMid;
        int inner = hovered ? theme.accent : theme.frameAccent;
        int top = hovered ? theme.headerTop : theme.sidebarBottom;
        int bottom = pressed ? theme.panelTop : theme.buttonBottom;

        graphics.fillGradient(
            getX(), getY(), getX() + width, getY() + height,
            top, bottom
        );

        graphics.renderOutline(getX(), getY(), width, height, outer);
        if (width > 4 && height > 4) {
            graphics.renderOutline(
                getX() + 1, getY() + 1, width - 2, height - 2, inner
            );
        }

        if (hovered) {
            graphics.fill(
                getX() + 2, getY() + 2, getX() + 3, getY() + height - 2,
                theme.accentBright
            );
        }

        var font = Minecraft.getInstance().font;
        Component text = bold
            ? getMessage().copy().withStyle(Style.EMPTY.withBold(true))
            : getMessage();

        int textWidth = font.width(text);
        graphics.drawString(
            font, text,
            getX() + (width - textWidth) / 2,
            getY() + (height - 8) / 2,
            hovered ? theme.accentBright : theme.text,
            false
        );
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        clickHandler.run();
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
