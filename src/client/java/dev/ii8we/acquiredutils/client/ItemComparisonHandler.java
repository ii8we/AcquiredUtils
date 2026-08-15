package dev.ii8we.acquiredutils.client;

import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compares item stats for the server-style tooltips used by AcquiredUtils.
 *
 * Compares server-defined item stats directly from the item tooltip.
 * Minecraft vanilla AttributeModifiers are intentionally ignored because the
 * target server uses its own stat system.
 */
public final class ItemComparisonHandler {

    private static final int MAX_LINES = 8;
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
        if (!AcquiredUtilsConfig.get().itemComparisonEnabled || player == null) {
            return;
        }

        Slot hovered = screen.hoveredSlot;
        if (hovered == null || !hovered.hasItem()) {
            return;
        }

        ItemStack candidate = hovered.getItem();
        EquipmentSlot equipmentSlot = player.getEquipmentSlotForItem(candidate);
        if (!player.isEquippableInSlot(candidate, equipmentSlot)) {
            return;
        }

        ItemStack equipped = player.getItemBySlot(equipmentSlot);
        if (equipped.isEmpty() || ItemStack.isSameItemSameComponents(candidate, equipped)) {
            return;
        }

        Map<StatKey, StatValue> candidateStats = collectStats(candidate, equipmentSlot, player);
        Map<StatKey, StatValue> equippedStats = collectStats(equipped, equipmentSlot, player);

        List<StatKey> keys = new ArrayList<>();
        keys.addAll(candidateStats.keySet());
        for (StatKey key : equippedStats.keySet()) {
            if (!keys.contains(key)) {
                keys.add(key);
            }
        }

        List<String> differences = new ArrayList<>();
        for (StatKey key : keys) {
            StatValue candidateValue = candidateStats.get(key);
            StatValue equippedValue = equippedStats.get(key);

            double candidateNumber = candidateValue == null ? 0.0 : candidateValue.value();
            double equippedNumber = equippedValue == null ? 0.0 : equippedValue.value();
            double delta = candidateNumber - equippedNumber;
            if (Math.abs(delta) < 0.0001) {
                continue;
            }

            boolean percentage = candidateValue != null
                ? candidateValue.percentage()
                : equippedValue.percentage();
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
        int panelX = 8;
        int panelY = Math.max(8, (screenH - panelH) / 2);

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

    private record StatKey(String normalizedName, String displayName) {
    }

    private record StatValue(double value, boolean percentage) {
    }

    private static Map<StatKey, StatValue> collectStats(ItemStack stack, EquipmentSlot slot, Player player) {
        Map<StatKey, StatValue> result = new LinkedHashMap<>();
        collectTooltipStats(stack, player, result);
        return result;
    }

    private static void collectTooltipStats(
        ItemStack stack,
        Player player,
        Map<StatKey, StatValue> result
    ) {
        if (player.level() == null) {
            return;
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
            if (displayName.isEmpty() || !looksLikeStatName(displayName)) {
                continue;
            }

            double value;
            try {
                value = Double.parseDouble(matcher.group(2).replace(',', '.'));
            } catch (NumberFormatException ignored) {
                continue;
            }

            boolean percentage = matcher.group(3) != null;
            StatKey key = new StatKey(normalizeStatName(displayName), displayName);
            result.put(key, new StatValue(value, percentage));
        }
    }

    private static String cleanupDisplayName(String value) {
        return value
            .replaceAll("^[^A-Za-z]+", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static boolean looksLikeStatName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return !lower.equals("item")
            && !lower.equals("price")
            && !lower.equals("cost")
            && !lower.equals("required level")
            && !lower.equals("rarity")
            && !lower.equals("level")
            && !lower.contains("click to")
            && !lower.contains("right click")
            && !lower.contains("left click");
    }

    private static String normalizeStatName(String name) {
        return name.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "")
            .trim();
    }

    private static String formatDelta(String displayName, double delta, boolean percentage) {
        double magnitude = Math.abs(delta);
        String number = magnitude >= 100 || magnitude == Math.rint(magnitude)
            ? String.format(Locale.ROOT, "%.0f", magnitude)
            : String.format(Locale.ROOT, "%.1f", magnitude);
        return (delta > 0 ? "+" : "-") + " " + displayName + ": " + number + (percentage ? "%" : "");
    }
}
