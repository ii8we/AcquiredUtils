package dev.bobodado.acquiredutils.client.gui.section;

import net.minecraft.client.gui.components.AbstractWidget;

public record GuiRow(
    String labelKey,
    String descKey,
    int descOffsetY,
    int controlWidth,
    int controlHeight,
    int rowSpacing,
    ControlFactory factory
) {

    @FunctionalInterface
    public interface ControlFactory {
        AbstractWidget create(int x, int y, int width, int height);
    }
}