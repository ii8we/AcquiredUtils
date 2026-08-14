package dev.bobodado.acquiredutils.client.recipe;

import dev.bobodado.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

public final class RecipeUnlockHighlightHandler {

    private static final String RECIPES_TITLE_PREFIX = "Recipes -";
    private static final int HIGHLIGHT_COLOR = 0xFF39D66F;
    private static final int HIGHLIGHT_BORDER_COLOR = 0xFF7CFF9B;

    private RecipeUnlockHighlightHandler() {
    }

    public static List<Slot> renderBackgrounds(
        GuiGraphics graphics,
        AbstractContainerScreen<?> screen,
        Player player,
        int leftPos,
        int topPos
    ) {
        List<Slot> highlighted = new ArrayList<>();

        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();
        if (!cfg.recipeUnlockHighlightEnabled || player == null || !isRecipeVault(screen)) {
            return highlighted;
        }

        for (Slot slot : screen.getMenu().slots) {
            // Only highlight slots belonging to the recipe vault itself.
            // Player inventory slots are part of the same menu, so explicitly exclude them.
            if (slot.container == player.getInventory()) {
                continue;
            }

            ItemStack stack = slot.getItem();
            if (!slot.isActive() || stack.isEmpty() || !isRecipeUnlocked(stack, player)) {
                continue;
            }

            int x = leftPos + slot.x;
            int y = topPos + slot.y;

            drawUnlockedTriangle(graphics, x, y);
            highlighted.add(slot);
        }

        return highlighted;
    }

    private static void drawUnlockedTriangle(
        GuiGraphics graphics,
        int x,
        int y
    ) {
        // Full 16x16 square overlay: exactly one vanilla slot.
        graphics.fill(
            x,
            y,
            x + 16,
            y + 16,
            0x3D39D66F
        );

        graphics.renderOutline(
            x,
            y,
            16,
            16,
            0xB87CFF9B
        );
    }

    private static boolean isRecipeVault(AbstractContainerScreen<?> screen) {
        String title = screen.getTitle().getString();
        return title.strip().toLowerCase(java.util.Locale.ROOT)
            .startsWith(RECIPES_TITLE_PREFIX.toLowerCase(java.util.Locale.ROOT));
    }

    private static boolean isRecipeUnlocked(ItemStack stack, Player player) {
        List<Component> tooltipLines = stack.getTooltipLines(
            Item.TooltipContext.of(player.level()),
            player,
            TooltipFlag.Default.NORMAL
        );

        for (Component line : tooltipLines) {
            String text = line.getString().stripLeading();
            if (text.contains("✓") || text.contains("✔")) {
                return true;
            }
        }

        return false;
    }
}
