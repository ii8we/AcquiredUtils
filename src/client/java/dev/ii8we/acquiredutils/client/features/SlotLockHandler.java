package dev.ii8we.acquiredutils.client.features;

import dev.ii8we.acquiredutils.AcquiredUtils;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;

/**
 * Client-side protection for player inventory slots.
 *
 * This class intentionally uses only Fabric API screen events. No mixins are
 * required, and the implementation is written against Mojang mappings for
 * Minecraft 1.21.11.
 */
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

            // Covers normal left/right/middle and extra mouse-button clicks.
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) ->
                allowMouseClick(containerScreen, event)
            );

            // Covers click-drag / quick-craft interaction. A drag is rejected
            // whenever its current slot is protected, so a locked slot cannot
            // be filled, emptied, or included in a quick-craft operation.
            ScreenMouseEvents.allowMouseDrag(screen).register((s, event, deltaX, deltaY) ->
                allowMouseDrag(containerScreen, event)
            );

            // Do not suppress releases: Minecraft needs the release event to
            // cleanly terminate an in-progress drag/quick-craft state.
            ScreenMouseEvents.allowMouseRelease(screen).register((s, event) -> true);

            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) ->
                allowKeyPress(containerScreen, event)
            );
        });

        // Protection for keyboard item-dropping while no container screen is open.
        // Hotbar selection itself is deliberately NOT blocked: locking a slot is
        // about protecting its contents, not preventing the player from holding it.
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!AcquiredUtilsConfig.get().slotLockEnabled) {
                return;
            }
            if (client.player == null || client.screen != null) {
                return;
            }

            int selectedSlot = client.player.getInventory().getSelectedSlot();
            if (!isLocked(selectedSlot)) {
                return;
            }

            Options options = client.options;
            options.keyDrop.consumeClick();
            options.keySwapOffhand.consumeClick();
        });
    }

    private static boolean allowMouseClick(AbstractContainerScreen<?> screen, MouseButtonEvent event) {
        if (!AcquiredUtilsConfig.get().slotLockEnabled) {
            return true;
        }

        Slot hovered = getSlotAt(screen, event.x(), event.y());
        return hovered == null || !isProtectedSlot(hovered);
    }

    private static boolean allowMouseDrag(AbstractContainerScreen<?> screen, MouseButtonEvent event) {
        if (!AcquiredUtilsConfig.get().slotLockEnabled) {
            return true;
        }

        Slot hovered = getSlotAt(screen, event.x(), event.y());
        return hovered == null || !isProtectedSlot(hovered);
    }

    private static boolean allowKeyPress(AbstractContainerScreen<?> screen, KeyEvent event) {
        if (!AcquiredUtilsConfig.get().slotLockEnabled) {
            return true;
        }

        // A focused vault-search field owns keyboard input. Never let slot-lock
        // keybinds (including F/offhand or a custom lock key) steal characters.
        if (InventorySearchHandler.isFocused(screen)) {
            return true;
        }

        int keyCode = event.key();
        if (keyCode < 0) {
            return true;
        }

        // The lock key itself is consumed by us so Minecraft does not also
        // process it as an inventory action.
        if (keyCode == AcquiredUtilsConfig.get().slotLockKey) {
            toggleHoveredSlot(screen);
            return false;
        }

        Options options = Minecraft.getInstance().options;
        Slot hovered = screen.hoveredSlot;

        // Q / drop: only relevant when the hovered slot belongs to the player.
        if (options.keyDrop.matches(event)) {
            return hovered == null || !isProtectedSlot(hovered);
        }

        // F / offhand swap can act on a container slot too, so always protect
        // the offhand target when it is locked.
        if (options.keySwapOffhand.matches(event)) {
            return !isLocked(40) && (hovered == null || !isProtectedSlot(hovered));
        }

        // Number-key swaps can act on ANY hovered container slot, with the
        // destination being one of the nine hotbar slots.
        for (int i = 0; i < options.keyHotbarSlots.length; i++) {
            if (options.keyHotbarSlots[i].matches(event)) {
                return !isLocked(i) && (hovered == null || !isProtectedSlot(hovered));
            }
        }

        return true;
    }

    public static java.util.List<Slot> renderOverlay(
        GuiGraphics graphics,
        AbstractContainerScreen<?> screen,
        int leftPos,
        int topPos
    ) {
        java.util.List<Slot> highlighted = new java.util.ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !AcquiredUtilsConfig.get().slotLockEnabled) {
            return highlighted;
        }

        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == mc.player.getInventory()
                && slot.isActive()
                && isLocked(slot.getContainerSlot())) {
                highlighted.add(slot);
            }
        }

        return highlighted;
    }

    /**
     * Draws locked-slot padlocks after item stacks and decorations have been
     * rendered, but before the container tooltip is submitted. This keeps the
     * lock icon visible over the item while the tooltip remains on top of it.
     */
    public static void renderIcons(
        GuiGraphics graphics,
        java.util.List<Slot> lockedSlots,
        int leftPos,
        int topPos
    ) {
        if (lockedSlots.isEmpty() || !AcquiredUtilsConfig.get().slotLockEnabled) {
            return;
        }

        for (Slot slot : lockedSlots) {
            drawPadlock(
                graphics,
                leftPos + slot.x,
                topPos + slot.y
            );
        }
    }

    private static void toggleHoveredSlot(AbstractContainerScreen<?> screen) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        Slot hovered = screen.hoveredSlot;
        if (hovered == null || !isPlayerInventorySlot(hovered) || !hovered.isActive()) {
            return;
        }

        int idx = hovered.getContainerSlot();
        if (!isValidPlayerInventoryIndex(idx)) {
            return;
        }

        java.util.Set<Integer> locked = AcquiredUtilsConfig.get().lockedSlots;
        boolean nowLocked;
        if (locked.contains(idx)) {
            locked.remove(idx);
            nowLocked = false;
        } else {
            locked.add(idx);
            nowLocked = true;
        }

        AcquiredUtilsConfig.save();
        AcquiredUtils.LOGGER.info(
            "[AcquiredUtils] Slot {} {}",
            idx,
            nowLocked ? "locked" : "unlocked"
        );
    }

    private static boolean isProtectedSlot(Slot slot) {
        return slot.isActive()
            && isPlayerInventorySlot(slot)
            && isLocked(slot.getContainerSlot());
    }

    private static boolean isPlayerInventorySlot(Slot slot) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && slot.container == mc.player.getInventory();
    }

    /**
     * Finds the menu slot under a raw screen-space mouse position.
     * This avoids depending on Minecraft's private mouse-position state and
     * works with the exact Mojang-mapped 1.21.11 menu coordinates.
     */
    private static Slot getSlotAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        double x = mouseX - screen.leftPos;
        double y = mouseY - screen.topPos;

        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive()) {
                continue;
            }

            if (x >= slot.x && x < slot.x + 16 && y >= slot.y && y < slot.y + 16) {
                return slot;
            }
        }

        return null;
    }

    private static boolean isValidPlayerInventoryIndex(int index) {
        return index >= 0 && index <= 40;
    }

    public static boolean isLocked(int containerSlotIndex) {
        return AcquiredUtilsConfig.get().lockedSlots.contains(containerSlotIndex);
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
