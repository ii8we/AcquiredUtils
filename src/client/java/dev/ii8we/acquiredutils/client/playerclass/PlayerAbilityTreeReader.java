package dev.ii8we.acquiredutils.client.playerclass;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reads the server-provided class abilities from the player's class ability tree vault.
 * AcquiredUtils never creates or changes abilities; it only caches what the server exposes.
 */
public final class PlayerAbilityTreeReader {
    private static final int SCAN_INTERVAL_TICKS = 5;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 36;
    private static final Pattern FORMATTING = Pattern.compile("\\u00A7[0-9A-FK-ORa-fk-or]");
    private static final Set<String> CLICK_WORDS = Set.of("LEFT", "RIGHT", "L", "R", "LEFT CLICK", "RIGHT CLICK");

    private static int tickCounter;
    private static int lastStateId = Integer.MIN_VALUE;
    private static String lastTitle = "";

    private PlayerAbilityTreeReader() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (++tickCounter < SCAN_INTERVAL_TICKS) return;
            tickCounter = 0;
            scanOpenAbilityTree(client);
        });
    }

    private static void scanOpenAbilityTree(Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (!(client.screen instanceof AbstractContainerScreen<?> screen)) return;

        String title = stripFormatting(screen.getTitle().getString()).trim();
        PlayerClass treeClass = detectClassFromAbilityTreeTitle(title);
        if (treeClass == null) {
            resetContainerState();
            return;
        }

        PlayerClass activeClass = PlayerClassDataManager.getActiveClass();
        if (activeClass != treeClass) {
            PlayerClassDataManager.setActiveClass(treeClass);
            activeClass = treeClass;
        }

        AbstractContainerMenu menu = screen.getMenu();
        int stateId = menu.getStateId();
        if (stateId == lastStateId && title.equals(lastTitle)) return;
        lastStateId = stateId;
        lastTitle = title;

        int containerEnd = findPlayerInventoryStart(menu, client);
        if (containerEnd <= 0) return;

        ItemStack classSword = findClassSword(menu, containerEnd, activeClass);
        if (classSword.isEmpty()) return;

        List<PlayerAbilityData> parsed = parseAbilities(client, classSword);
        PlayerClassData data = PlayerClassDataManager.getData(activeClass);
        if (data == null) return;

        if (!sameAbilities(data.abilities, parsed)) {
            data.abilities = parsed;
            PlayerClassDataManager.saveClass(activeClass);
        }
    }

    private static int findPlayerInventoryStart(AbstractContainerMenu menu, Minecraft client) {
        if (client.player != null) {
            try {
                return menu.findSlot(client.player.getInventory(), 0).orElse(menu.slots.size() - PLAYER_INVENTORY_SLOT_COUNT);
            } catch (Exception ignored) {
                // Fall through to the standard 36-slot player-inventory layout.
            }
        }
        return Math.max(0, menu.slots.size() - PLAYER_INVENTORY_SLOT_COUNT);
    }

    private static ItemStack findClassSword(AbstractContainerMenu menu, int containerEnd, PlayerClass playerClass) {
        String expectedName = playerClass.displayName();
        int limit = Math.min(containerEnd, menu.slots.size());

        // The server provides a diamond sword named exactly after the class.
        // Check the container first so the player's own inventory cannot win.
        for (int index = 0; index < limit; index++) {
            ItemStack stack = menu.slots.get(index).getItem();
            if (isClassSword(stack, expectedName)) return stack.copy();
        }
        return ItemStack.EMPTY;
    }

    private static boolean isClassSword(ItemStack stack, String expectedName) {
        if (stack.isEmpty() || !stack.is(Items.DIAMOND_SWORD)) return false;
        String hoverName = stripFormatting(stack.getHoverName().getString()).trim();
        if (hoverName.equalsIgnoreCase(expectedName)) return true;
        // Be tolerant of server formatting such as "Warrior" plus a trailing marker.
        return hoverName.replaceAll("\\s+", " ").trim().equalsIgnoreCase(expectedName);
    }

    private static List<PlayerAbilityData> parseAbilities(Minecraft client, ItemStack classSword) {
        List<PlayerAbilityData> abilities = new ArrayList<>();
        List<Component> tooltip = classSword.getTooltipLines(
                Item.TooltipContext.of(client.level),
                client.player,
                TooltipFlag.NORMAL
        );

        boolean inCoreAbilities = false;
        Set<String> seen = new HashSet<>();
        for (Component component : tooltip) {
            String line = stripFormatting(component.getString()).trim();
            if (line.isEmpty()) continue;

            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("core abilities")) {
                inCoreAbilities = true;
                continue;
            }
            if (!inCoreAbilities) continue;

            // Stop when the tooltip enters another named section.
            if (lower.equals("abilities") || lower.contains("passive abilities") || lower.equals("stats")) continue;

            PlayerAbilityData ability = parseAbilityLine(line);
            if (ability == null) continue;

            String key = ability.name.toLowerCase(Locale.ROOT) + "\u0000" + ability.clicks.toUpperCase(Locale.ROOT);
            if (seen.add(key)) abilities.add(ability);
        }

        return abilities;
    }

    private static PlayerAbilityData parseAbilityLine(String line) {
        String cleaned = line.replace('\u00A0', ' ').trim();
        List<String> clicks = new ArrayList<>();

        // Supports server tooltip formats such as:
        // "LEFT · RIGHT · Ability Name", "Ability Name [LEFT · RIGHT]",
        // "Ability Name [L R]", and "Ability Name - LEFT RIGHT".
        java.util.regex.Matcher bracket = Pattern.compile("\\[([^\\]]+)\\]").matcher(cleaned);
        String namePart = cleaned;
        if (bracket.find()) {
            clicks.addAll(extractClicks(bracket.group(1)));
            namePart = (cleaned.substring(0, bracket.start()) + cleaned.substring(bracket.end())).trim();
        }

        if (clicks.isEmpty()) {
            String[] parts = cleaned.split("\\s*[·•]\\s*", -1);
            int prefix = 0;
            while (prefix < parts.length && isClick(parts[prefix])) {
                clicks.add(normalizeClick(parts[prefix]));
                prefix++;
            }
            if (!clicks.isEmpty() && prefix < parts.length) {
                namePart = String.join(" · ", java.util.Arrays.copyOfRange(parts, prefix, parts.length)).trim();
            }
        }

        if (clicks.isEmpty()) {
            java.util.regex.Matcher suffix = Pattern.compile("(?:\\s*[-:|]\\s*)(.+)$").matcher(cleaned);
            if (suffix.find()) {
                List<String> possible = extractClicks(suffix.group(1));
                if (!possible.isEmpty()) {
                    clicks.addAll(possible);
                    namePart = cleaned.substring(0, suffix.start()).trim();
                }
            }
        }

        if (clicks.isEmpty()) return null;
        String abilityName = namePart.replaceAll("\\s{2,}", " ").trim();
        if (abilityName.isBlank()) return null;
        return new PlayerAbilityData(abilityName, String.join(" · ", clicks));
    }

    private static List<String> extractClicks(String text) {
        List<String> clicks = new ArrayList<>();
        String normalized = text.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]+", " ").trim();
        if (normalized.isEmpty()) return clicks;
        for (String token : normalized.split("\\s+")) {
            if (token.equals("LEFT") || token.equals("L")) clicks.add("LEFT");
            else if (token.equals("RIGHT") || token.equals("R")) clicks.add("RIGHT");
            else if (token.equals("CLICK")) {
                // LEFT CLICK / RIGHT CLICK is handled by the preceding token.
            }
        }
        return clicks;
    }

    private static boolean isClick(String value) {
        String v = value.trim().toUpperCase(Locale.ROOT);
        return v.equals("LEFT") || v.equals("RIGHT") || v.equals("L") || v.equals("R")
                || v.equals("LEFT CLICK") || v.equals("RIGHT CLICK");
    }

    private static String normalizeClick(String value) {
        String v = value.trim().toUpperCase(Locale.ROOT);
        return v.startsWith("L") ? "LEFT" : "RIGHT";
    }

    private static boolean sameAbilities(List<PlayerAbilityData> left, List<PlayerAbilityData> right) {
        if (left == null || right == null || left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++) {
            PlayerAbilityData a = left.get(i);
            PlayerAbilityData b = right.get(i);
            String aName = a == null || a.name == null ? "" : a.name.trim();
            String aClicks = a == null || a.clicks == null ? "" : a.clicks.trim();
            String bName = b == null || b.name == null ? "" : b.name.trim();
            String bClicks = b == null || b.clicks == null ? "" : b.clicks.trim();
            if (!aName.equals(bName) || !aClicks.equals(bClicks)) return false;
        }
        return true;
    }

    private static PlayerClass detectClassFromAbilityTreeTitle(String title) {
        for (PlayerClass playerClass : PlayerClass.values()) {
            String prefix = playerClass.displayName() + " Ability Tree";
            if (title.regionMatches(true, 0, prefix, 0, prefix.length())) {
                return playerClass;
            }
        }
        return null;
    }

    private static String stripFormatting(String text) {
        return FORMATTING.matcher(text == null ? "" : text).replaceAll("");
    }

    private static void resetContainerState() {
        lastStateId = Integer.MIN_VALUE;
        lastTitle = "";
    }
}
