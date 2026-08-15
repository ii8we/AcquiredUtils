package dev.ii8we.acquiredutils.client.gui.widget;

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

public final class ThemedCheckboxWidget extends AbstractWidget {

    private static final int CHECKBOX_SIZE = 12;

    private static final Identifier CHECKED_TEXTURE =
        Identifier.fromNamespaceAndPath(
            "acquiredutils",
            "textures/gui/checkbox_purple_checked.png"
        );

    private static final Identifier UNCHECKED_TEXTURE =
        Identifier.fromNamespaceAndPath(
            "acquiredutils",
            "textures/gui/checkbox_purple_unchecked.png"
        );

    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;

    public ThemedCheckboxWidget(
        int x,
        int y,
        int width,
        int height,
        BooleanSupplier getter,
        Consumer<Boolean> setter
    ) {
        super(x, y, width, height, Component.empty());
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    protected void renderWidget(
        GuiGraphics graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        boolean checked = getter.getAsBoolean();

        int boxX = getX() + (width - CHECKBOX_SIZE) / 2;
        int boxY = getY() + (height - CHECKBOX_SIZE) / 2;

        Identifier texture = checked ? CHECKED_TEXTURE : UNCHECKED_TEXTURE;
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            boxX,
            boxY,
            0.0F,
            0.0F,
            CHECKBOX_SIZE,
            CHECKBOX_SIZE,
            CHECKBOX_SIZE,
            CHECKBOX_SIZE
        );
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        setter.accept(!getter.getAsBoolean());
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.literal(
            getter.getAsBoolean() ? "Enabled" : "Disabled"
        ));
    }
}
