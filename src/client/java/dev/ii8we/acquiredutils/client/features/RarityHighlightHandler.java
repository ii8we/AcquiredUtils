package dev.ii8we.acquiredutils.client.features;

import dev.ii8we.acquiredutils.AcquiredUtils;
import dev.ii8we.acquiredutils.client.compat.ServerCompatibility;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

public final class RarityHighlightHandler {

    private static final Identifier[] RARITY_GRADIENT_TEXTURES = new Identifier[] {
        Identifier.fromNamespaceAndPath(AcquiredUtils.MOD_ID, "textures/gui/rarity/common_gradient.png"),
        Identifier.fromNamespaceAndPath(AcquiredUtils.MOD_ID, "textures/gui/rarity/uncommon_gradient.png"),
        Identifier.fromNamespaceAndPath(AcquiredUtils.MOD_ID, "textures/gui/rarity/rare_gradient.png"),
        Identifier.fromNamespaceAndPath(AcquiredUtils.MOD_ID, "textures/gui/rarity/epic_gradient.png"),
        Identifier.fromNamespaceAndPath(AcquiredUtils.MOD_ID, "textures/gui/rarity/legendary_gradient.png"),
        Identifier.fromNamespaceAndPath(AcquiredUtils.MOD_ID, "textures/gui/rarity/mythic_gradient.png")
    };

    private static final int SLOT_SIZE = 16;

    private RarityHighlightHandler() {
    }

    public static void init() {
        // Rendering is coordinated by ContainerOverlayHandler.
    }

    public static java.util.List<Slot> renderOverlay(
        GuiGraphics graphics,
        AbstractContainerScreen<?> screen,
        Player player,
        int leftPos,
        int topPos
    ) {
        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();

        if (!ServerCompatibility.isFeatureAllowed("rarity_highlight") || player == null || !cfg.rarityCircleEnabled) {
            return new java.util.ArrayList<>();
        }

        java.util.List<Slot> highlighted = new java.util.ArrayList<>();

        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive() || slot.getItem().isEmpty()) {
                continue;
            }

            ItemRarity rarity = ItemRarityDetector.detect(slot.getItem(), player);
            if (rarity == null) {
                continue;
            }

            int slotX = leftPos + slot.x;
            int slotY = topPos + slot.y;

            drawRarityGradient(graphics, slotX, slotY, rarity);
            highlighted.add(slot);
        }

        return highlighted;
    }

    private static void drawRarityGradient(
        GuiGraphics graphics,
        int slotX,
        int slotY,
        ItemRarity rarity
    ) {
        int index = rarity.ordinal();
        if (index < 0 || index >= RARITY_GRADIENT_TEXTURES.length) {
            return;
        }

        // Full 16x16 slot texture. The PNG itself contains the vertical alpha
        // gradient: transparent at the top, strongest around the middle/lower
        // portion, and still visible toward the bottom. The item is redrawn
        // afterwards by ContainerOverlayHandler.
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            RARITY_GRADIENT_TEXTURES[index],
            slotX,
            slotY,
            0.0f,
            0.0f,
            SLOT_SIZE,
            SLOT_SIZE,
            SLOT_SIZE,
            SLOT_SIZE
        );
    }
}
