package dev.bobodado.acquiredutils.client;

import dev.bobodado.acquiredutils.client.pickup.RarityHighlightHandler;
import dev.bobodado.acquiredutils.client.recipe.RecipeUnlockHighlightHandler;
import dev.bobodado.acquiredutils.config.AcquiredUtilsConfig;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

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

        List<Slot> rarityHighlighted = RarityHighlightHandler.renderBackgrounds(
            graphics, screen, player, leftPos, topPos
        );
        for (Slot slot : rarityHighlighted) {
            renderSlotItem(graphics, slot, leftPos, topPos);
        }

        List<Slot> recipeHighlighted = RecipeUnlockHighlightHandler.renderBackgrounds(
            graphics, screen, player, leftPos, topPos
        );
        for (Slot slot : recipeHighlighted) {
            renderSlotItem(graphics, slot, leftPos, topPos);
        }

        SlotLockHandler.renderOverlay(graphics, screen, leftPos, topPos);

        int tooltipLineCount = renderHoveredTooltip(graphics, screen, player, mouseX, mouseY);
        ItemComparisonHandler.render(
            graphics,
            screen,
            player,
            mouseX,
            mouseY,
            tooltipLineCount
        );
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

    private static int renderHoveredTooltip(
        GuiGraphics graphics,
        AbstractContainerScreen<?> screen,
        Player player,
        int mouseX,
        int mouseY
    ) {
        Slot hovered = screen.hoveredSlot;
        if (hovered == null || !hovered.hasItem()) {
            return 0;
        }

        ItemStack stack = hovered.getItem();
        List<Component> lines = stack.getTooltipLines(
            Item.TooltipContext.of(player.level()),
            player,
            TooltipFlag.Default.NORMAL
        );

        if (lines.isEmpty()) {
            return 0;
        }

        List<ClientTooltipComponent> components = new ArrayList<>(lines.size());
        for (Component line : lines) {
            components.add(ClientTooltipComponent.create(line.getVisualOrderText()));
        }

        graphics.renderTooltip(
            Minecraft.getInstance().font,
            components,
            mouseX,
            mouseY,
            DefaultTooltipPositioner.INSTANCE,
            null
        );

        return components.size();
    }
}
