package dev.ii8we.acquiredutils.client.features;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ii8we.acquiredutils.AcquiredUtils;
import dev.ii8we.acquiredutils.client.compat.ServerCompatibility;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.block.model.BakedQuad;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Rarity glint for held items. */
public final class RarityGlintHandler {

    private static final Map<ItemRarity, RenderType> GLINT_RENDER_TYPES = new EnumMap<>(ItemRarity.class);

    static {
        for (ItemRarity rarity : ItemRarity.values()) {
            GLINT_RENDER_TYPES.put(rarity, createRenderType(rarity));
        }
    }

    private RarityGlintHandler() {
    }

    public static boolean appliesToHeldItem(ItemStack stack, Player player) {
        return AcquiredUtilsConfig.get().rarityGlintEnabled
            && player != null
            && stack != null
            && !stack.isEmpty()
            && ItemRarityDetector.detect(stack, player) != null;
    }

    /**
     * Replaces the normal item foil on a held-item render state with a custom,
     * rarity-specific animated glint layer.
     */
    public static void applyHeldItemGlint(ItemStackRenderState renderState, ItemStack stack, Player player) {
        if (!ServerCompatibility.isFeatureAllowed("rarity_glint") || !appliesToHeldItem(stack, player) || renderState.isEmpty()) {
            return;
        }

        ItemRarity rarity = ItemRarityDetector.detect(stack, player);
        if (rarity == null) {
            return;
        }

        RenderType glintRenderType = GLINT_RENDER_TYPES.get(rarity);
        if (glintRenderType == null) {
            return;
        }

        int originalLayerCount = renderState.activeLayerCount;
        for (int i = 0; i < originalLayerCount; i++) {
            ItemStackRenderState.LayerRenderState original = renderState.layers[i];
            List<BakedQuad> quads = original.prepareQuadList();
            if (quads.isEmpty()) {
                continue;
            }

            // Suppress vanilla enchantment foil on the original layer.
            original.setFoilType(ItemStackRenderState.FoilType.NONE);

            // Add a second pass using the exact same baked quads but the
            // AcquiredUtils animated rarity texture and vanilla GLINT pipeline.
            ItemStackRenderState.LayerRenderState glintLayer = renderState.newLayer();
            glintLayer.prepareQuadList().addAll(quads);
            glintLayer.setRenderType(glintRenderType);
            glintLayer.setFoilType(ItemStackRenderState.FoilType.NONE);
        }
    }

    private static RenderType createRenderType(ItemRarity rarity) {
        Identifier texture = Identifier.fromNamespaceAndPath(
            AcquiredUtils.MOD_ID,
            "textures/gui/rarity_glint/" + rarity.name().toLowerCase(java.util.Locale.ROOT) + ".png"
        );

        RenderSetup setup = RenderSetup.builder(RenderPipelines.GLINT)
            .withTexture("Sampler0", texture)
            .setTextureTransform(TextureTransform.GLINT_TEXTURING)
            .createRenderSetup();

        return RenderType.create("acquiredutils:rarity_glint/" + rarity.name().toLowerCase(java.util.Locale.ROOT), setup);
    }
}
