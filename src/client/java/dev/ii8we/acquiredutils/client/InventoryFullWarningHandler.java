package dev.ii8we.acquiredutils.client;

import dev.ii8we.acquiredutils.AcquiredUtils;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Displays a small warning above the hotbar when all 36 normal storage slots
 * (hotbar + main inventory) are occupied.
 */
public final class InventoryFullWarningHandler {

    private static final Identifier HUD_ID =
        Identifier.fromNamespaceAndPath(AcquiredUtils.MOD_ID, "inventory_full_warning");

    private static final Component WARNING = Component.literal("Inventory Full");

    private InventoryFullWarningHandler() {
    }

    public static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, HUD_ID, (graphics, tickCounter) -> {
            Minecraft client = Minecraft.getInstance();
            if (!AcquiredUtilsConfig.get().inventoryFullWarningEnabled
                || client.player == null
                || client.screen != null
                || client.options.hideGui
                || !isStorageInventoryFull(client.player.getInventory())) {
                return;
            }

            render(graphics);
        });
    }

    private static boolean isStorageInventoryFull(Inventory inventory) {
        for (int slot = 0; slot < 36; slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int textWidth = client.font.width(WARNING);
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 50;

        graphics.drawString(client.font, WARNING, x, y, 0xFFFF5555, true);
    }
}
