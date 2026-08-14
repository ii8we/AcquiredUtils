package dev.bobodado.acquiredutils.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.bobodado.acquiredutils.config.AcquiredUtilsConfig;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

import java.util.Locale;

public final class InventorySearchHandler {

    private static String query = "";
    private static boolean searching = false;

    private InventorySearchHandler() {
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof InventoryScreen inventoryScreen)) {
                return;
            }

            searching = false;
            query = "";

            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> {
                int keyCode = InputConstants.getKey(event).getValue();
                AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();

                if (!searching && keyCode == cfg.inventorySearchKey) {
                    searching = true;
                    query = "";
                    return false;
                }

                if (!searching) {
                    return true;
                }

                if (keyCode == InputConstants.KEY_ESCAPE) {
                    searching = false;
                    query = "";
                    return false;
                }

                if (keyCode == InputConstants.KEY_BACKSPACE) {
                    if (!query.isEmpty()) {
                        query = query.substring(0, query.length() - 1);
                    }
                    return false;
                }

                char typed = toCharacter(keyCode);
                if (typed != 0 && query.length() < 32) {
                    query += typed;
                    return false;
                }

                return true;
            });

            ScreenEvents.afterRender(screen).register(
                (s, graphics, mouseX, mouseY, partialTick) ->
                    render(graphics, inventoryScreen)
            );
        });
    }

    private static void render(
        GuiGraphics graphics,
        InventoryScreen screen
    ) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || !searching) {
            return;
        }

        int x = screen.leftPos;
        int y = screen.topPos - 22;

        int w = 170;
        int h = 18;

        graphics.fill(x, y, x + w, y + h, 0xE60B0712);
        graphics.renderOutline(x, y, w, h, 0xFF7C5C9E);

        String text = query.isEmpty() ? "Search inventory..." : "Search: " + query;
        graphics.drawString(
            mc.font,
            Component.literal(text),
            x + 5,
            y + 5,
            query.isEmpty() ? 0xFFAA9FB0 : 0xFFF2EAF7,
            false
        );

        String normalized = query.toLowerCase(Locale.ROOT);

        for (Slot slot : screen.getMenu().slots) {
            if (slot.container != player.getInventory() || !slot.hasItem()) {
                continue;
            }

            if (!normalized.isEmpty()
                && slot.getItem().getHoverName().getString().toLowerCase(Locale.ROOT).contains(normalized)) {

                int sx = screen.leftPos + slot.x;
                int sy = screen.topPos + slot.y;

                graphics.fill(
                    sx,
                    sy,
                    sx + 16,
                    sy + 16,
                    0x3D9A6CFF
                );
                graphics.renderOutline(
                    sx,
                    sy,
                    16,
                    16,
                    0xFFBFA4FF
                );
            }
        }
    }

    private static char toCharacter(int keyCode) {
        if (keyCode >= InputConstants.KEY_A && keyCode <= InputConstants.KEY_Z) {
            return (char) ('a' + (keyCode - InputConstants.KEY_A));
        }

        if (keyCode >= InputConstants.KEY_0 && keyCode <= InputConstants.KEY_9) {
            return (char) ('0' + (keyCode - InputConstants.KEY_0));
        }

        if (keyCode == InputConstants.KEY_SPACE) {
            return ' ';
        }

        if (keyCode == InputConstants.KEY_MINUS) {
            return '-';
        }

        return 0;
    }
}
