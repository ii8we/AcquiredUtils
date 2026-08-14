package dev.bobodado.acquiredutils.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.bobodado.acquiredutils.AcquiredUtils;
import dev.bobodado.acquiredutils.config.AcquiredUtilsConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;

public final class SlotLockHandler {

    private static final Identifier LOCK_TEXTURE =
        Identifier.fromNamespaceAndPath(
            AcquiredUtils.MOD_ID,
            "textures/gui/lock.png"
        );

    private SlotLockHandler() {
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
                return;
            }

            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
                if (!AcquiredUtilsConfig.get().slotLockEnabled) return true;

                Slot hovered = containerScreen.hoveredSlot;
                if (hovered == null || !isPlayerInventorySlot(hovered) || !hovered.isActive()) {
                    return true;
                }

                return !isLocked(hovered.getContainerSlot());
            });

            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> {
                if (!AcquiredUtilsConfig.get().slotLockEnabled) return true;

                int keyCode = InputConstants.getKey(event).getValue();
                if (keyCode < 0) return true;
                Options options = Minecraft.getInstance().options;

                if (keyCode == AcquiredUtilsConfig.get().slotLockKey && keyCode >= 0) {
                    toggleHoveredSlot(containerScreen);
                    return false;
                }

                Slot hovered = containerScreen.hoveredSlot;
                if (hovered == null || !isPlayerInventorySlot(hovered) || !hovered.isActive()) {
                    return true;
                }

                int hoveredSlot = hovered.getContainerSlot();

                if (options.keyDrop.matches(event)) {
                    return !isLocked(hoveredSlot);
                }

                if (options.keySwapOffhand.matches(event)) {
                    return !isLocked(hoveredSlot) && !isLocked(40);
                }

                for (int i = 0; i < 9; i++) {
                    if (options.keyHotbarSlots[i].matches(event)) {
                        return !isLocked(hoveredSlot) && !isLocked(i);
                    }
                }

                return true;
            });
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!AcquiredUtilsConfig.get().slotLockEnabled) return;
            if (client.player == null || client.screen != null) return;

            int selectedSlot = client.player.getInventory().getSelectedSlot();
            if (!isLocked(selectedSlot)) return;

            Options options = client.options;
            options.keyDrop.consumeClick();
            options.keySwapOffhand.consumeClick();
        });
    }

    public static void renderOverlay(
        GuiGraphics graphics,
        AbstractContainerScreen<?> screen,
        int leftPos,
        int topPos
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !AcquiredUtilsConfig.get().slotLockEnabled) return;

        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == mc.player.getInventory()
                && slot.isActive()
                && isLocked(slot.getContainerSlot())) {

                drawPadlock(
                    graphics,
                    leftPos + slot.x,
                    topPos + slot.y
                );
            }
        }
    }

    private static void toggleHoveredSlot(AbstractContainerScreen<?> screen) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Slot hovered = screen.hoveredSlot;
        if (hovered == null || !isPlayerInventorySlot(hovered) || !hovered.isActive()) return;

        int idx = hovered.getContainerSlot();
        java.util.Set<Integer> locked = AcquiredUtilsConfig.get().lockedSlots;

        if (idx < 0 || idx > 40) return;

        if (locked.contains(idx)) {
            locked.remove(idx);
        } else {
            locked.add(idx);
        }

        AcquiredUtilsConfig.save();
        AcquiredUtils.LOGGER.info(
            "[AcquiredUtils] Slot {} {}",
            idx,
            locked.contains(idx) ? "locked" : "unlocked"
        );
    }

    private static boolean isPlayerInventorySlot(Slot slot) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && slot.container == mc.player.getInventory();
    }

    public static boolean isLocked(int containerSlotIndex) {
        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();

        return cfg.lockedSlots.contains(containerSlotIndex);
    }

    private static void drawPadlock(GuiGraphics graphics, int x, int y) {
        graphics.blit(
            net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
            LOCK_TEXTURE,
            x,
            y,
            0.0f,
            0.0f,
            16,
            16,
            16,
            16
        );
    }
}
