package dev.bobodado.acquiredutils.client.gui.widget;

import dev.bobodado.acquiredutils.client.gui.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Compact themed checkbox matching the Slot Lock control. */
public final class ThemedCheckboxWidget extends AbstractWidget {
    private static final int SIZE = 12;
    private static final Identifier CHECKED = Identifier.fromNamespaceAndPath(
        "acquiredutils", "textures/gui/checkbox_purple_checked.png"
    );
    private static final Identifier UNCHECKED = Identifier.fromNamespaceAndPath(
        "acquiredutils", "textures/gui/checkbox_purple_unchecked.png"
    );

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
        int size = Math.min(SIZE, Math.min(width, height));
        int x = getX() + (width - size) / 2;
        int y = getY() + (height - size) / 2;
        Identifier texture = getter.getAsBoolean() ? CHECKED : UNCHECKED;

        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, size, size, SIZE, SIZE);

        if (isHovered()) {
            graphics.renderOutline(x - 1, y - 1, size + 2, size + 2, Theme.current().accentBright);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        setter.accept(!getter.getAsBoolean());
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.literal(getter.getAsBoolean() ? "Enabled" : "Disabled"));
    }
}
