package dev.ii8we.acquiredutils.client.pickup;

import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

public final class RarityHighlightHandler {

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

        if (player == null || !cfg.rarityCircleEnabled) {
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

            drawRarityBorder(
                graphics,
                slotX,
                slotY,
                rarity.color()
            );

            highlighted.add(slot);
        }

        return highlighted;
    }

    private static void drawRarityBorder(
        GuiGraphics graphics,
        int x,
        int y,
        int color
    ) {
        // Fixed 1px border around the full 16x16 item slot.
        final int borderWidth = 1;

        graphics.fill(x, y, x + 16, y + borderWidth, color);
        graphics.fill(x, y + 16 - borderWidth, x + 16, y + 16, color);
        graphics.fill(x, y, x + borderWidth, y + 16, color);
        graphics.fill(x + 16 - borderWidth, y, x + 16, y + 16, color);
    }
}
