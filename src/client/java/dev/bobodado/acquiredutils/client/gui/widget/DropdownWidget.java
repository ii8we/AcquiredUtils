package dev.bobodado.acquiredutils.client.gui.widget;

import dev.bobodado.acquiredutils.client.gui.theme.Theme;
import dev.bobodado.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public class DropdownWidget extends AbstractWidget {

    private static final int ROW_HEIGHT = 12;

    private final List<Component> options;
    private final Consumer<Integer> onSelect;
    private int selectedIndex;
    private boolean open;

    public DropdownWidget(
        int x,
        int y,
        int width,
        int height,
        List<Component> options,
        int initialSelectedIndex,
        Consumer<Integer> onSelect
    ) {
        super(x, y, width, height, options.get(initialSelectedIndex));
        this.options = options;
        this.selectedIndex = initialSelectedIndex;
        this.onSelect = onSelect;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
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

        int background = isHovered()
            ? theme.headerTop
            : theme.headerBottom;

        graphics.fill(
            getX(),
            getY(),
            getX() + width,
            getY() + height,
            background
        );

        graphics.renderOutline(
            getX(),
            getY(),
            width,
            height,
            isHovered() ? theme.accentBright : theme.frameMid
        );

        graphics.drawString(
            font,
            options.get(selectedIndex),
            getX() + 4,
            getY() + (height - 8) / 2,
            theme.text,
            false
        );

        graphics.drawString(
            font,
            "▾",
            getX() + width - 10,
            getY() + (height - 8) / 2,
            theme.accentBright,
            false
        );
    }

    public void renderOverlay(
        GuiGraphics graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        if (!open) return;

        Theme theme = Theme.current();

        int listY = getY() + height;
        int rowHeight = ROW_HEIGHT;
        int listHeight = options.size() * rowHeight;

        graphics.fill(
            getX(),
            listY,
            getX() + width,
            listY + listHeight,
            theme.panelBottom
        );

        graphics.renderOutline(
            getX(),
            listY,
            width,
            listHeight,
            theme.accentBright
        );

        for (int i = 0; i < options.size(); i++) {
            int rowY = listY + i * rowHeight;

            if (i == selectedIndex) {
                graphics.fill(
                    getX() + 1,
                    rowY,
                    getX() + width - 1,
                    rowY + rowHeight,
                    theme.tabActiveBg
                );
            }

            graphics.drawString(
                Minecraft.getInstance().font,
                options.get(i),
                getX() + 4,
                rowY + 2,
                theme.text,
                false
            );
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (open) {
            int listY = getY() + height;
            int rowHeight = ROW_HEIGHT;
            int listHeight = options.size() * rowHeight;

            if (mouseX >= getX()
                && mouseX < getX() + width
                && mouseY >= listY
                && mouseY < listY + listHeight) {

                int index = ((int) mouseY - listY) / rowHeight;

                if (index >= 0 && index < options.size()) {
                    selectedIndex = index;
                    setMessage(options.get(index));
                    onSelect.accept(index);
                }

                open = false;
                return;
            }

            open = false;
            return;
        }

        if (isMouseOver(mouseX, mouseY)) {
            open = true;
        }
    }

    public boolean isOverExpandedArea(double mouseX, double mouseY) {
        if (!open) {
            return isMouseOver(mouseX, mouseY);
        }

        int listY = getY() + height;
        int listHeight = options.size() * ROW_HEIGHT;

        return mouseX >= getX()
            && mouseX < getX() + width
            && mouseY >= listY
            && mouseY < listY + listHeight;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(
            NarratedElementType.TITLE,
            options.get(selectedIndex)
        );
    }
}
