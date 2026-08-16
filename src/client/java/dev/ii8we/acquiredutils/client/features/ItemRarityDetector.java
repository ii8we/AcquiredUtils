package dev.ii8we.acquiredutils.client.features;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class ItemRarityDetector {

    private record CacheKey(String itemId, int componentsHash) {}

    private static final Pattern[] RARITY_PATTERNS = {
        Pattern.compile("^COMMON(?:\\s|$)"),
        Pattern.compile("^UNCOMMON(?:\\s|$)"),
        Pattern.compile("^RARE(?:\\s|$)"),
        Pattern.compile("^EPIC(?:\\s|$)"),
        Pattern.compile("^LEGENDARY(?:\\s|$)"),
        Pattern.compile("^MYTHIC(?:\\s|$)")
    };

    private static final int MAX_CACHE_ENTRIES = 256;
    private static final Map<CacheKey, ItemRarity> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, ItemRarity> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };

    private ItemRarityDetector() {
    }

    public static ItemRarity detect(ItemStack stack, Player player) {
        if (stack == null || stack.isEmpty() || player == null) {
            return null;
        }

        CacheKey cacheKey = new CacheKey(
            BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
            stack.getComponents().hashCode()
        );
        synchronized (CACHE) {
            if (CACHE.containsKey(cacheKey)) {
                return CACHE.get(cacheKey);
            }
        }

        /*
         * The SMP stores rarity in the lore/tooltip text, for example:
         * "EPIC CHESTPLATE".
         *
         * The item's display-name color is intentionally NOT used here.
         * This prevents ordinary items with a colored name from being
         * incorrectly marked as a rarity item.
         *
         * Tooltip line 0 is the item's display name, so start at line 1
         * and inspect only the remaining tooltip/lore lines.
         */
        var tooltipLines = stack.getTooltipLines(
            Item.TooltipContext.of(player.level()),
            player,
            TooltipFlag.Default.NORMAL
        );

        for (int i = 1; i < tooltipLines.size(); i++) {
            String line = tooltipLines.get(i).getString().trim();
            ItemRarity rarity = detectLoreLine(line);

            if (rarity != null) {
                synchronized (CACHE) {
                    CACHE.put(cacheKey, rarity);
                }
                return rarity;
            }
        }

        synchronized (CACHE) {
            CACHE.put(cacheKey, null);
        }
        return null;
    }

    private static ItemRarity detectLoreLine(String text) {
        String upper = text.toUpperCase(Locale.ROOT);

        for (int i = 0; i < RARITY_PATTERNS.length; i++) {
            if (RARITY_PATTERNS[i].matcher(upper).find()) {
                return ItemRarity.values()[i];
            }
        }

        return null;
    }
    public static void clearCache() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }
}

