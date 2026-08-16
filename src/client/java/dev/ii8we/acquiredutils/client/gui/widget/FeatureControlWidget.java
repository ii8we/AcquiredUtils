package dev.ii8we.acquiredutils.client.gui.widget;

import dev.ii8we.acquiredutils.client.features.FeatureRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class FeatureControlWidget extends AbstractWidget {
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

    private final String featureId;
    private final Runnable onToggle;

    public FeatureControlWidget(
        int x,
        int y,
        int width,
        int height,
        String featureId,
        Runnable onToggle
    ) {
        super(x, y, width, height, Component.empty());
        this.featureId = featureId;
        this.onToggle = onToggle;
    }

    @Override
    protected void renderWidget(
        GuiGraphics graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        var feature = FeatureRegistry.get(featureId);
        boolean checked = feature != null && feature.isEnabled();

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
        var feature = FeatureRegistry.get(featureId);
        if (feature != null) {
            feature.setEnabled(!feature.isEnabled());
            dev.ii8we.acquiredutils.config.AcquiredUtilsConfig.get().markDirty();
            onToggle.run();
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        var feature = FeatureRegistry.get(featureId);
        boolean enabled = feature != null && feature.isEnabled();
        output.add(
            NarratedElementType.TITLE,
            Component.literal(enabled ? "Enabled" : "Disabled")
        );
    }
}
