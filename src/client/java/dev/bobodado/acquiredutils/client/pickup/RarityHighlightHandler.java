package dev.bobodado.acquiredutils.client.pickup;

import dev.bobodado.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

public final class RarityHighlightHandler {

    private RarityHighlightHandler() {
    }

    public static void init() {
        // Rendering is coordinated by ContainerOverlayHandler.
    }

    public static List<Slot> renderBackgrounds(
        GuiGraphics graphics,
        AbstractContainerScreen<?> screen,
        Player player,
        int leftPos,
        int topPos
    ) {
        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();

        List<Slot> highlighted = new ArrayList<>();
        if (player == null || !cfg.rarityCircleEnabled) {
            return highlighted;
        }
        int radius = Math.max(3, Math.min(8, Math.round(cfg.rarityCircleSize)));
        int alpha = Math.max(0, Math.min(255, Math.round(cfg.rarityCircleOpacity * 255.0f)));

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

            drawRarityCircle(
                graphics,
                slotX + 8,
                slotY + 8,
                rarity.color(),
                radius,
                alpha
            );
            highlighted.add(slot);
        }

        return highlighted;
    }

    private static void drawRarityCircle(
        GuiGraphics graphics,
        int centerX,
        int centerY,
        int color,
        int outerRadius,
        int alpha
    ) {
        int argb = (alpha << 24) | (color & 0x00FFFFFF);

        // Filled translucent disk; it is rendered before the item is redrawn.
        for (int dy = -outerRadius; dy <= outerRadius; dy++) {
            int width = (int) Math.floor(Math.sqrt(outerRadius * outerRadius - dy * dy));
            graphics.fill(
                centerX - width,
                centerY + dy,
                centerX + width + 1,
                centerY + dy + 1,
                argb
            );
        }
    }
}
