package dev.bobodado.acquiredutils.client.gui.widget;

import dev.bobodado.acquiredutils.client.gui.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.BooleanSupplier;

public final class ThemedCheckboxWidget extends AbstractWidget {

    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;

    public ThemedCheckboxWidget(int x, int y, int width, int height,
                                BooleanSupplier getter, Consumer<Boolean> setter) {
        super(x, y, width, height, Component.empty());
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Theme theme = Theme.current();
        int size = Math.min(12, Math.max(10, height - 4));
        int x = getX() + (width - size) / 2;
        int y = getY() + (height - size) / 2 - 2;

        int fill = getter.getAsBoolean() ? theme.accent : theme.footerBottom;
        int border = isHovered() ? theme.accentBright : theme.frameMid;

        graphics.fill(x, y, x + size, y + size, fill);
        graphics.renderOutline(x, y, size, size, border);

        if (getter.getAsBoolean()) {
            graphics.drawString(
                net.minecraft.client.Minecraft.getInstance().font,
                Component.literal("✓"),
                x + 2,
                y + 1,
                theme.text,
                false
            );
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        setter.accept(!getter.getAsBoolean());
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.translatable("acquiredutils.gui.checkbox.toggle"));
    }
}
