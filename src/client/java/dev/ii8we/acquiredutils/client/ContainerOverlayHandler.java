package dev.ii8we.acquiredutils.client;

import dev.ii8we.acquiredutils.client.pickup.RarityHighlightHandler;
import dev.ii8we.acquiredutils.client.recipe.RecipeUnlockHighlightHandler;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ContainerOverlayHandler {

    private ContainerOverlayHandler() {
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
                return;
            }

            ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, partialTick) ->
                render(graphics, containerScreen, mouseX, mouseY)
            );
        });
    }

    private static void render(
        GuiGraphics graphics,
        AbstractContainerScreen<?> screen,
        int mouseX,
        int mouseY
    ) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }

        int leftPos = screen.leftPos;
        int topPos = screen.topPos;

        // Draw all slot-local overlays first. Then move to a new GUI stratum and
        // redraw the affected item stacks so their models, counts, and enchantment
        // decorations are always above our overlays.
        List<Slot> searchHighlighted = InventorySearchHandler.renderHighlights(graphics, screen);

        List<Slot> recipeHighlighted = RecipeUnlockHighlightHandler.renderBackgrounds(
            graphics,
            screen,
            player,
            leftPos,
            topPos
        );

        List<Slot> rarityHighlighted = RarityHighlightHandler.renderOverlay(
            graphics, screen, player, leftPos, topPos
        );

        List<Slot> locked = SlotLockHandler.renderOverlay(
            graphics, screen, leftPos, topPos
        );

        Set<Slot> redraw = new LinkedHashSet<>();
        redraw.addAll(searchHighlighted);
        redraw.addAll(recipeHighlighted);
        redraw.addAll(rarityHighlighted);
        redraw.addAll(locked);

        if (!redraw.isEmpty()) {
            graphics.nextStratum();
            for (Slot slot : redraw) {
                renderSlotItem(graphics, slot, leftPos, topPos);
            }
        }

        // Slot-lock is intentionally the only slot overlay drawn above the
        // item itself. The tooltip is submitted immediately afterwards, so
        // the item's description remains on top of the lock icon.
        SlotLockHandler.renderIcons(graphics, locked, leftPos, topPos);

        ItemComparisonHandler.render(
            graphics,
            screen,
            player,
            mouseX,
            mouseY
        );

        // On 1.21.11 the ScreenEvents after-render hook can run after the normal
        // deferred tooltip pass. Re-submit and immediately render the current
        // container tooltip so it is guaranteed to sit above every slot overlay.
        screen.renderTooltip(graphics, mouseX, mouseY);
        graphics.renderDeferredElements();
    }

    private static void renderSlotItem(
        GuiGraphics graphics,
        Slot slot,
        int leftPos,
        int topPos
    ) {
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return;
        }

        int x = leftPos + slot.x;
        int y = topPos + slot.y;

        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(Minecraft.getInstance().font, stack, x, y);
    }



}
