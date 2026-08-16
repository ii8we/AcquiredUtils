package dev.ii8we.acquiredutils.client.features;

import dev.ii8we.acquiredutils.AcquiredUtils;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import dev.ii8we.acquiredutils.client.overlay.OverlayManager;
import dev.ii8we.acquiredutils.client.overlay.OverlayPosition;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ItemPickupNotifier {

    private static final Identifier HUD_ID =
        Identifier.fromNamespaceAndPath(AcquiredUtils.MOD_ID, "item_pickup_notifier");

    private static final List<PickupNotification> NOTIFICATIONS = new ArrayList<>();
    private static List<ItemStack> previousInventory = null;

    private ItemPickupNotifier() {
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(ItemPickupNotifier::tick);

        OverlayManager.register(HUD_ID, () -> new OverlayPosition(AcquiredUtilsConfig.get().notificationPositionX, AcquiredUtilsConfig.get().notificationPositionY), (graphics, tickCounter, width, height, position) -> render(graphics, width, height, position));
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, HUD_ID, (graphics, tickCounter) ->
            OverlayManager.renderAll(graphics, tickCounter, Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight())
        );
    }

    private static void tick(Minecraft client) {
        if (client.player == null) {
            previousInventory = null;
            ItemRarityDetector.clearCache();
            NOTIFICATIONS.clear();
            return;
        }

        if (!AcquiredUtilsConfig.get().itemPickupNotifierEnabled) {
            previousInventory = snapshot(client.player.getInventory());
            NOTIFICATIONS.clear();
            return;
        }

        List<ItemStack> current = snapshot(client.player.getInventory());

        if (previousInventory == null) {
            previousInventory = current;
            return;
        }

        if (client.screen == null) {
            detectGains(previousInventory, current);
        }

        previousInventory = current;
        removeExpired(System.currentTimeMillis());
    }

    private static List<ItemStack> snapshot(net.minecraft.world.entity.player.Inventory inventory) {
        int size = inventory.getContainerSize();
        List<ItemStack> result = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            result.add(inventory.getItem(i).copy());
        }

        return result;
    }

    private static void detectGains(List<ItemStack> previous, List<ItemStack> current) {
        List<ItemStack> processed = new ArrayList<>();

        for (ItemStack stack : current) {
            if (stack.isEmpty() || containsSame(processed, stack)) {
                continue;
            }

            int previousCount = countMatching(previous, stack);
            int currentCount = countMatching(current, stack);
            int gained = currentCount - previousCount;

            if (gained > 0) {
                ItemRarity rarity = ItemRarityDetector.detect(stack, Minecraft.getInstance().player);
                if (rarity != null) {
                    addNotification(gained, stack, rarity);
                }
            }

            processed.add(stack);
        }
    }

    private static int countMatching(List<ItemStack> stacks, ItemStack target) {
        int total = 0;
        for (ItemStack stack : stacks) {
            if (ItemStack.isSameItemSameComponents(stack, target)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static boolean containsSame(List<ItemStack> stacks, ItemStack target) {
        for (ItemStack stack : stacks) {
            if (ItemStack.isSameItemSameComponents(stack, target)) return true;
        }
        return false;
    }

    private static void addNotification(int amount, ItemStack stack, ItemRarity rarity) {
        String name = stack.getHoverName().getString();
        String text = amount + "x " + name;
        long expiresAt = System.currentTimeMillis() +
            (long) (AcquiredUtilsConfig.get().notificationDuration * 1000.0f);

        NOTIFICATIONS.add(new PickupNotification(text, rarity.color(), expiresAt));
        while (NOTIFICATIONS.size() > 5) {
            NOTIFICATIONS.remove(0);
        }
    }

    private static void removeExpired(long now) {
        Iterator<PickupNotification> iterator = NOTIFICATIONS.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().expiresAt() <= now) {
                iterator.remove();
            }
        }
    }

    private static void render(GuiGraphics graphics, int screenWidth, int screenHeight, OverlayPosition position) {
        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();
        if (!cfg.itemPickupNotifierEnabled) return;

        removeExpired(System.currentTimeMillis());
        if (NOTIFICATIONS.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;

        float px = position == null ? cfg.notificationPositionX : position.x();
        float py = position == null ? cfg.notificationPositionY : position.y();
        int centerX = Math.round(px * screenWidth);
        int startY = Math.round(py * screenHeight);

        int y = startY;
        for (PickupNotification notification : NOTIFICATIONS) {
            Component text = Component.literal(notification.text());
            int width = client.font.width(text);
            int drawX;

            if (px < 0.5f) {
                drawX = centerX;
            } else if (px > 0.5f) {
                drawX = centerX - width;
            } else {
                drawX = centerX - width / 2;
            }

            graphics.drawString(
                client.font,
                text,
                drawX,
                y,
                notification.color(),
                true
            );
            y += 12;
        }
    }
}
