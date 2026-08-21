package dev.ii8we.acquiredutils.client.features;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.ii8we.acquiredutils.AcquiredUtils;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Client-only first-person item renderer that preserves vanilla's rendering path
 * while adding configurable per-hand transforms.
 *
 * The important detail here is that the vanilla first-person camera/bobbing
 * transforms are applied before any AcquiredUtils transforms. Minecraft's
 * ItemInHandRenderer does this in renderHandsWithItems(), and omitting those
 * transforms makes custom-positioned items stop tracking the camera correctly.
 */
public final class PositionedItemInHandRenderer extends ItemInHandRenderer {

    private static boolean installed;

    private PositionedItemInHandRenderer(Minecraft client, ItemInHandRenderer original) {
        super(client, original.entityRenderDispatcher, original.itemModelResolver);

        // Preserve the live renderer state when the replacement is installed.
        this.mainHandItem = original.mainHandItem.copy();
        this.offHandItem = original.offHandItem.copy();
        this.mainHandHeight = original.mainHandHeight;
        this.oMainHandHeight = original.oMainHandHeight;
        this.offHandHeight = original.offHandHeight;
        this.oOffHandHeight = original.oOffHandHeight;
    }

    /**
     * Installs exactly once, after Minecraft's GameRenderer and hand renderer exist.
     */
    public static void ensureInstalled() {
        if (installed) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.gameRenderer == null || client.gameRenderer.itemInHandRenderer == null) {
            return;
        }
        if (client.gameRenderer.itemInHandRenderer instanceof PositionedItemInHandRenderer) {
            installed = true;
            return;
        }

        ItemInHandRenderer original = client.gameRenderer.itemInHandRenderer;
        client.gameRenderer.itemInHandRenderer = new PositionedItemInHandRenderer(client, original);
        installed = true;
        AcquiredUtils.LOGGER.info("[AcquiredUtils] Installed no-Mixin first-person item position renderer");
    }

    @Override
    public void renderItem(
        net.minecraft.world.entity.LivingEntity mob,
        ItemStack itemStack,
        net.minecraft.world.item.ItemDisplayContext type,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords
    ) {
        if (itemStack.isEmpty()) {
            return;
        }

        net.minecraft.client.renderer.item.ItemStackRenderState renderState = new net.minecraft.client.renderer.item.ItemStackRenderState();
        this.itemModelResolver.updateForTopItem(
            renderState,
            itemStack,
            type,
            mob.level(),
            mob,
            mob.getId() + type.ordinal()
        );

        if (mob == Minecraft.getInstance().player) {
            RarityGlintHandler.applyHeldItemGlint(renderState, itemStack, (net.minecraft.world.entity.player.Player) mob);
        }

        renderState.submit(poseStack, submitNodeCollector, lightCoords, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0);
    }

    @Override
    public void renderHandsWithItems(
        float tickProgress,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        LocalPlayer player,
        int light
    ) {
        AcquiredUtilsConfig config = AcquiredUtilsConfig.get();

        // Keep the vanilla path only when neither held-item feature needs it.
        if (!config.itemPositionEnabled && !config.rarityGlintEnabled) {
            super.renderHandsWithItems(tickProgress, poseStack, submitNodeCollector, player, light);
            return;
        }

        // Maps use a coordinated vanilla path. Do not replace it with the normal
        // per-hand item path because that would break Minecraft's two-handed map
        // rendering logic.
        if (this.mainHandItem.is(Items.FILLED_MAP) || this.offHandItem.is(Items.FILLED_MAP)) {
            super.renderHandsWithItems(tickProgress, poseStack, submitNodeCollector, player, light);
            return;
        }

        // These two transforms are part of vanilla 1.21.11's first-person render
        // path. They must be preserved so the held item remains camera-relative.
        float xBob = Mth.lerp(tickProgress, player.xBobO, player.xBob);
        float yBob = Mth.lerp(tickProgress, player.yBobO, player.yBob);
        poseStack.mulPose(Axis.XP.rotationDegrees((player.getViewXRot(tickProgress) - xBob) * 0.1F));
        poseStack.mulPose(Axis.YP.rotationDegrees((player.getViewYRot(tickProgress) - yBob) * 0.1F));

        float attackProgress = player.getAttackAnim(tickProgress);
        InteractionHand swingingHand = player.swingingArm == null
            ? InteractionHand.MAIN_HAND
            : player.swingingArm;
        float pitch = player.getXRot(tickProgress);
        HandRenderSelection selection = evaluateWhichHandsToRender(player);

        if (selection.renderMainHand) {
            float swingProgress = swingingHand == InteractionHand.MAIN_HAND ? attackProgress : 0.0F;
            float equipProgress = this.itemModelResolver.swapAnimationScale(this.mainHandItem)
                * (1.0F - Mth.lerp(tickProgress, this.oMainHandHeight, this.mainHandHeight));

            renderConfiguredHand(
                player,
                tickProgress,
                pitch,
                InteractionHand.MAIN_HAND,
                swingProgress,
                this.mainHandItem,
                equipProgress,
                poseStack,
                submitNodeCollector,
                light,
                config,
                true
            );
        }

        if (selection.renderOffHand) {
            float swingProgress = swingingHand == InteractionHand.OFF_HAND ? attackProgress : 0.0F;
            float equipProgress = this.itemModelResolver.swapAnimationScale(this.offHandItem)
                * (1.0F - Mth.lerp(tickProgress, this.oOffHandHeight, this.offHandHeight));

            renderConfiguredHand(
                player,
                tickProgress,
                pitch,
                InteractionHand.OFF_HAND,
                swingProgress,
                this.offHandItem,
                equipProgress,
                poseStack,
                submitNodeCollector,
                light,
                config,
                false
            );
        }

        // Match vanilla's end-of-pass work because this override replaces the
        // complete renderHandsWithItems() method.
        Minecraft client = Minecraft.getInstance();
        client.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
        client.renderBuffers().bufferSource().endBatch();
    }

    private void renderConfiguredHand(
        LocalPlayer player,
        float tickProgress,
        float pitch,
        InteractionHand hand,
        float swingProgress,
        ItemStack itemStack,
        float equipProgress,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int light,
        AcquiredUtilsConfig config,
        boolean mainHand
    ) {
        // Empty hands are not items. Do not move/rotate the player arm when only
        // the held-item feature is enabled.
        if (!config.itemPositionEnabled || itemStack.isEmpty()) {
            renderArmWithItem(
                player,
                tickProgress,
                pitch,
                hand,
                swingProgress,
                itemStack,
                equipProgress,
                poseStack,
                submitNodeCollector,
                light
            );
            return;
        }

        poseStack.pushPose();
        applyTransform(poseStack, config, mainHand);
        renderArmWithItem(
            player,
            tickProgress,
            pitch,
            hand,
            swingProgress,
            itemStack,
            equipProgress,
            poseStack,
            submitNodeCollector,
            light
        );
        poseStack.popPose();
    }

    private static void applyTransform(PoseStack poseStack, AcquiredUtilsConfig config, boolean mainHand) {
        float x = mainHand ? config.mainHandPositionX : config.offhandPositionX;
        float y = mainHand ? config.mainHandPositionY : config.offhandPositionY;
        float z = mainHand ? config.mainHandPositionZ : config.offhandPositionZ;
        float rotX = mainHand ? config.mainHandRotationX : config.offhandRotationX;
        float rotY = mainHand ? config.mainHandRotationY : config.offhandRotationY;
        float rotZ = mainHand ? config.mainHandRotationZ : config.offhandRotationZ;
        float scale = mainHand ? config.mainHandScale : config.offhandScale;

        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));
        poseStack.scale(scale, scale, scale);
    }
}