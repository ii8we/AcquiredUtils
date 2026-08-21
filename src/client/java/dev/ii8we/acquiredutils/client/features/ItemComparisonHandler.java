package dev.ii8we.acquiredutils.client.features;

import dev.ii8we.acquiredutils.client.compat.ServerCompatibility;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.TridentItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compares only meaningful stats between items of the same equipment category
 * and concrete item type. A sword is never compared against a pickaxe, an axe
 * against a shovel, or a helmet against a chestplate.
 */
public final class ItemComparisonHandler {

    private static final int MAX_LINES = 6;
    private static final Pattern STAT_LINE_PATTERN = Pattern.compile(
        "^\\s*(?:[^A-Za-z0-9]*)([A-Za-z][A-Za-z0-9' _-]{1,40}?)\\s*:\\s*([+-]?\\d+(?:[.,]\\d+)?)\\s*(%)?.*$"
    );

    private ItemComparisonHandler() {
    }

    public static void render(
        GuiGraphics graphics,
        AbstractContainerScreen<?> screen,
        Player player,
        int mouseX,
        int mouseY
    ) {
        if (!ServerCompatibility.isFeatureAllowed("gear_comparison") || !AcquiredUtilsConfig.get().itemComparisonEnabled || player == null) {
            return;
        }

        SlotComparison comparison = findComparison(screen, player);
        if (comparison == null) {
            return;
        }

        Map<StatKey, StatValue> candidateStats = collectStats(comparison.candidate(), player, comparison.type());
        Map<StatKey, StatValue> equippedStats = collectStats(comparison.equipped(), player, comparison.type());

        List<StatKey> keys = new ArrayList<>();
        for (StatKey key : candidateStats.keySet()) {
            keys.add(key);
        }
        for (StatKey key : equippedStats.keySet()) {
            if (!keys.contains(key)) {
                keys.add(key);
            }
        }

        List<String> differences = new ArrayList<>();
        for (StatKey key : keys) {
            StatValue candidateValue = candidateStats.get(key);
            StatValue equippedValue = equippedStats.get(key);

            // Compare only stats that both items actually expose. This keeps the
            if (candidateValue == null || equippedValue == null) {
                continue;
            }

            double delta = candidateValue.value() - equippedValue.value();
            if (Math.abs(delta) < 0.0001) {
                continue;
            }

            boolean percentage = candidateValue.percentage();

            differences.add(formatDelta(key.displayName(), delta, percentage));
            if (differences.size() >= MAX_LINES) {
                break;
            }
        }

        if (differences.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int panelW = 190;
        int panelH = 18 + differences.size() * 10;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // Fixed middle-left comparison panel.
        int panelX = 8;
        int panelY = Math.max(4, (screenH - panelH) / 2);

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xE60B0712);
        graphics.renderOutline(panelX, panelY, panelW, panelH, 0xFF7C5C9E);
        graphics.drawString(
            mc.font,
            Component.translatable("acquiredutils.gui.compare.title"),
            panelX + 6,
            panelY + 5,
            0xFFE5B85C,
            true
        );

        int y = panelY + 16;
        for (String line : differences) {
            int color = line.startsWith("+") ? 0xFF6DFF9B : 0xFFFF7F91;
            graphics.drawString(mc.font, Component.literal(line), panelX + 6, y, color, false);
            y += 10;
        }
    }

    private static SlotComparison findComparison(AbstractContainerScreen<?> screen, Player player) {
        if (screen.hoveredSlot == null || !screen.hoveredSlot.hasItem()) {
            return null;
        }

        ItemStack candidate = screen.hoveredSlot.getItem();
        ComparisonType type = classify(candidate);
        if (type == ComparisonType.NONE) {
            return null;
        }

        EquipmentSlot targetSlot = resolveTargetSlot(candidate, type, player);
        if (targetSlot == null) {
            return null;
        }

        ItemStack equipped = player.getItemBySlot(targetSlot);
        if (equipped.isEmpty() || ItemStack.isSameItemSameComponents(candidate, equipped)) {
            return null;
        }

        if (classify(equipped) != type) {
            return null;
        }

        return new SlotComparison(candidate, equipped, type);
    }

    private static EquipmentSlot resolveTargetSlot(ItemStack stack, ComparisonType type, Player player) {
        if (type.category() == Category.ARMOR) {
            EquipmentSlot preferred = player.getEquipmentSlotForItem(stack);
            return preferred.isArmor() ? preferred : null;
        }

        ItemStack mainHand = player.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!mainHand.isEmpty() && classify(mainHand) == type) {
            return EquipmentSlot.MAINHAND;
        }

        ItemStack offHand = player.getItemBySlot(EquipmentSlot.OFFHAND);
        if (!offHand.isEmpty() && classify(offHand) == type) {
            return EquipmentSlot.OFFHAND;
        }

        return null;
    }

    private static ComparisonType classify(ItemStack stack) {
        Item item = stack.getItem();

        EquipmentSlot preferredSlot = Minecraft.getInstance().player == null
            ? EquipmentSlot.MAINHAND
            : Minecraft.getInstance().player.getEquipmentSlotForItem(stack);
        if (preferredSlot.isArmor()) {
            return switch (preferredSlot) {
                case HEAD -> ComparisonType.HELMET;
                case CHEST -> ComparisonType.CHESTPLATE;
                case LEGS -> ComparisonType.LEGGINGS;
                case FEET -> ComparisonType.BOOTS;
                default -> ComparisonType.NONE;
            };
        }

        String itemPath = BuiltInRegistries.ITEM.getKey(item).getPath().toLowerCase(Locale.ROOT);
        if (itemPath.contains("katana")) {
            return ComparisonType.KATANA;
        }
        if (itemPath.endsWith("_sword") || itemPath.contains("sword")) {
            return ComparisonType.SWORD;
        }
        if (item instanceof MaceItem) {
            return ComparisonType.MACE;
        }
        if (item instanceof BowItem) {
            return ComparisonType.BOW;
        }
        if (item instanceof CrossbowItem) {
            return ComparisonType.CROSSBOW;
        }
        if (item instanceof TridentItem) {
            return ComparisonType.TRIDENT;
        }
        if (itemPath.endsWith("_pickaxe") || itemPath.contains("pickaxe")) {
            return ComparisonType.PICKAXE;
        }
        if (item instanceof AxeItem) {
            return ComparisonType.AXE;
        }
        if (item instanceof ShovelItem) {
            return ComparisonType.SHOVEL;
        }
        if (item instanceof HoeItem) {
            return ComparisonType.HOE;
        }
        if (item instanceof ShearsItem) {
            return ComparisonType.SHEARS;
        }

        return ComparisonType.NONE;
    }

    private static Map<StatKey, StatValue> collectStats(
        ItemStack stack,
        Player player,
        ComparisonType type
    ) {
        Map<StatKey, StatValue> result = new LinkedHashMap<>();
        if (player.level() == null) {
            return result;
        }

        List<Component> lines = stack.getTooltipLines(
            Item.TooltipContext.of(player.level()),
            player,
            TooltipFlag.Default.NORMAL
        );

        for (Component line : lines) {
            String text = line.getString().trim();
            Matcher matcher = STAT_LINE_PATTERN.matcher(text);
            if (!matcher.matches()) {
                continue;
            }

            String displayName = cleanupDisplayName(matcher.group(1));
            if (!isRelevantStat(type, displayName)) {
                continue;
            }

            double value;
            try {
                value = Double.parseDouble(matcher.group(2).replace(',', '.'));
            } catch (NumberFormatException ignored) {
                continue;
            }

            boolean percentage = matcher.group(3) != null;
            result.put(
                new StatKey(normalizeStatName(displayName), displayName),
                new StatValue(value, percentage)
            );
        }

        return result;
    }

    private static boolean isRelevantStat(ComparisonType type, String name) {
        String lower = normalizeStatName(name);
        if (lower.isEmpty()) {
            return false;
        }

        return switch (type.category()) {
            case WEAPON -> containsAny(lower,
                "damage",
                "attackdamage",
                "strength",
                "critchance",
                "critdamage",
                "criticalchance",
                "criticaldamage",
                "attackspeed",
                "ferocity",
                "mana",
                "abilitydamage",
                "abilitystrength",
                "speed",
                "health",
                "defense");
            case ARMOR -> containsAny(lower,
                "health",
                "defense",
                "armor",
                "armortoughness",
                "strength",
                "speed",
                "critchance",
                "critdamage",
                "criticalchance",
                "criticaldamage",
                "ferocity",
                "mana",
                "knockbackresistance");
            case TOOL -> containsAny(lower,
                "miningspeed",
                "breakingspeed",
                "fortune",
                "miningfortune",
                "attackdamage",
                "damage",
                "attackspeed",
                "strength",
                "durability",
                "speed");
        };
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String cleanupDisplayName(String value) {
        return value
            .replaceAll("^[^A-Za-z]+", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static String normalizeStatName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "")
            .trim();

        return switch (normalized) {
            case "attackdamage" -> "damage";
            case "criticalchance" -> "critchance";
            case "criticaldamage" -> "critdamage";
            case "miningfortune" -> "fortune";
            case "miningspeedmultiplier" -> "miningspeed";
            default -> normalized;
        };
    }

    private static String formatDelta(String displayName, double delta, boolean percentage) {
        double magnitude = Math.abs(delta);
        String number = magnitude >= 100 || magnitude == Math.rint(magnitude)
            ? String.format(Locale.ROOT, "%.0f", magnitude)
            : String.format(Locale.ROOT, "%.1f", magnitude);
        return (delta > 0 ? "+" : "-") + " " + displayName + ": " + number + (percentage ? "%" : "");
    }

    private enum Category {
        WEAPON,
        ARMOR,
        TOOL
    }

    private enum ComparisonType {
        SWORD(Category.WEAPON),
        KATANA(Category.WEAPON),
        MACE(Category.WEAPON),
        BOW(Category.WEAPON),
        CROSSBOW(Category.WEAPON),
        TRIDENT(Category.WEAPON),
        HELMET(Category.ARMOR),
        CHESTPLATE(Category.ARMOR),
        LEGGINGS(Category.ARMOR),
        BOOTS(Category.ARMOR),
        PICKAXE(Category.TOOL),
        AXE(Category.TOOL),
        SHOVEL(Category.TOOL),
        HOE(Category.TOOL),
        SHEARS(Category.TOOL),
        NONE(null);

        private final Category category;

        ComparisonType(Category category) {
            this.category = category;
        }

        Category category() {
            return category;
        }
    }

    private record StatKey(String normalizedName, String displayName) {
    }

    private record StatValue(double value, boolean percentage) {
    }

    private record SlotComparison(ItemStack candidate, ItemStack equipped, ComparisonType type) {
    }
}
