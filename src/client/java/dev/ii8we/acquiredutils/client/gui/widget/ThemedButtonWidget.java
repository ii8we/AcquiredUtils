package dev.ii8we.acquiredutils.client.gui.widget;

import dev.ii8we.acquiredutils.client.gui.theme.Theme;
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
        boolean focused = isFocused();

        int outer = hovered ? theme.accentBright : (focused ? theme.accent : theme.frameMid);
        int top = hovered ? theme.headerTop : theme.sidebarTop;
        int bottom = hovered ? theme.buttonBottom : theme.sidebarBottom;

        graphics.fillGradient(
            getX() + 1, getY() + 1,
            getX() + width - 1, getY() + height - 1,
            top, bottom
        );
        graphics.renderOutline(getX(), getY(), width, height, outer);

        if (width > 6 && height > 6) {
            graphics.renderOutline(
                getX() + 2, getY() + 2, width - 4, height - 4,
                hovered ? theme.frameAccent : theme.frameMid
            );
        }

        if (hovered) {
            graphics.fill(
                getX() + 3, getY() + 3,
                getX() + width - 3, getY() + 4,
                0x55FFFFFF
            );
        }

        var font = Minecraft.getInstance().font;
        Component text = bold
            ? getMessage().copy().withStyle(Style.EMPTY.withBold(true))
            : getMessage();

        int textWidth = font.width(text);
        int textY = getY() + Math.max(1, (height - font.lineHeight) / 2);
        graphics.drawString(
            font,
            text,
            getX() + (width - textWidth) / 2,
            textY,
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
