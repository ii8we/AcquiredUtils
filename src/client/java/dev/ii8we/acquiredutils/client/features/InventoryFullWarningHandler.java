package dev.ii8we.acquiredutils.client.features;

import dev.ii8we.acquiredutils.AcquiredUtils;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Shows a short, centered alert when the player's normal storage inventory becomes full.
 */
public final class InventoryFullWarningHandler {

    private static final Identifier HUD_ID =
        Identifier.fromNamespaceAndPath(AcquiredUtils.MOD_ID, "inventory_full_warning");

    private static final Component WARNING =
        Component.literal("Inventory Full").withStyle(Style.EMPTY.withBold(true));
    private static final Component SUBTITLE =
        Component.literal("There is no room left in your inventory.");

    private static final long ALERT_DURATION_MS = 2500L;
    private static final long FADE_DURATION_MS = 250L;

    private static boolean wasFull;
    private static long alertUntil;

    private InventoryFullWarningHandler() {
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(InventoryFullWarningHandler::tick);

        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, HUD_ID, (graphics, tickCounter) -> {
            Minecraft client = Minecraft.getInstance();
            if (!AcquiredUtilsConfig.get().inventoryFullWarningEnabled
                || client.player == null
                || client.screen != null
                || client.options.hideGui
                || alertUntil <= System.currentTimeMillis()) {
                return;
            }

            render(graphics);
        });
    }

    private static void tick(Minecraft client) {
        long now = System.currentTimeMillis();

        if (client.player == null || !AcquiredUtilsConfig.get().inventoryFullWarningEnabled) {
            wasFull = false;
            alertUntil = 0L;
            return;
        }

        boolean full = isStorageInventoryFull(client.player.getInventory());
        if (full && !wasFull) {
            alertUntil = now + ALERT_DURATION_MS;
        }

        wasFull = full;
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
        long remaining = alertUntil - System.currentTimeMillis();
        if (remaining <= 0L) {
            return;
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        float fadeIn = Mth.clamp((ALERT_DURATION_MS - remaining) / (float) FADE_DURATION_MS, 0.0F, 1.0F);
        float fadeOut = Mth.clamp(remaining / (float) FADE_DURATION_MS, 0.0F, 1.0F);
        int alpha = (int) (255.0F * Math.min(fadeIn, fadeOut));
        if (alpha <= 0) {
            return;
        }

        int titleWidth = client.font.width(WARNING);
        int subtitleWidth = client.font.width(SUBTITLE);
        int contentWidth = Math.max(titleWidth, subtitleWidth);

        int panelWidth = contentWidth + 32;
        int panelHeight = 46;
        int panelX = (screenWidth - panelWidth) / 2;
        int panelY = (screenHeight - panelHeight) / 2;

        int panelColor = (alpha * 160 / 255 << 24) | 0x120B19;
        int borderColor = alpha << 24 | 0xC26CFF;
        int accentColor = alpha << 24 | 0xFF5555;
        int titleColor = alpha << 24 | 0xFFFFFF;
        int subtitleColor = (alpha * 220 / 255 << 24) | 0xD8D0E0;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, panelColor);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, borderColor);
        graphics.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, borderColor);
        graphics.fill(panelX, panelY, panelX + 2, panelY + panelHeight, accentColor);
        graphics.fill(panelX + panelWidth - 2, panelY, panelX + panelWidth, panelY + panelHeight, borderColor);

        graphics.drawString(
            client.font,
            WARNING,
            panelX + (panelWidth - titleWidth) / 2,
            panelY + 8,
            titleColor,
            true
        );
        graphics.drawString(
            client.font,
            SUBTITLE,
            panelX + (panelWidth - subtitleWidth) / 2,
            panelY + 25,
            subtitleColor,
            true
        );
    }
}
