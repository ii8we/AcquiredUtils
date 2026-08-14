package dev.bobodado.acquiredutils.client;

import dev.bobodado.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ItemComparisonHandler {

    private static final Pattern NUMERIC_STAT =
        Pattern.compile("^\\s*([^:]{2,40}):\\s*([+-]?\\d+(?:\\.\\d+)?)");

    private ItemComparisonHandler() {
    }

    public static void render(
        GuiGraphics graphics,
        AbstractContainerScreen<?> screen,
        Player player,
        int mouseX,
        int mouseY,
        int tooltipLineCount
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

        Map<String, Double> candidateStats = parseStats(candidate, player);
        Map<String, Double> equippedStats = parseStats(equipped, player);

        if (candidateStats.isEmpty() || equippedStats.isEmpty()) {
            return;
        }

        List<String> differences = new ArrayList<>();
        for (Map.Entry<String, Double> entry : candidateStats.entrySet()) {
            Double oldValue = equippedStats.get(entry.getKey());
            if (oldValue == null) {
                continue;
            }

            double delta = entry.getValue() - oldValue;
            if (Math.abs(delta) < 0.0001) {
                continue;
            }

            differences.add(formatDelta(entry.getKey(), delta));
            if (differences.size() >= 5) {
                break;
            }
        }

        if (differences.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int panelW = 150;
        int panelH = 18 + differences.size() * 10;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int panelX = 8;
        int panelY = mouseY - panelH / 2;

        panelY = Math.max(8, Math.min(screenH - panelH - 8, panelY));

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xE60B0712);
        graphics.renderOutline(panelX, panelY, panelW, panelH, 0xFF7C5C9E);

        graphics.drawString(
            mc.font,
            Component.literal("Comparison"),
            panelX + 6,
            panelY + 5,
            0xFFE5B85C,
            true
        );

        int y = panelY + 16;
        for (String line : differences) {
            int color = line.startsWith("+") ? 0xFF6DFF9B : 0xFFFF7F91;
            graphics.drawString(
                mc.font,
                Component.literal(line),
                panelX + 6,
                y,
                color,
                false
            );
            y += 10;
        }
    }

    private static Map<String, Double> parseStats(ItemStack stack, Player player) {
        List<Component> lines = stack.getTooltipLines(
            Item.TooltipContext.of(player.level()),
            player,
            TooltipFlag.Default.NORMAL
        );

        Map<String, Double> result = new LinkedHashMap<>();

        for (int i = 1; i < lines.size(); i++) {
            Matcher matcher = NUMERIC_STAT.matcher(lines.get(i).getString());
            if (!matcher.find()) {
                continue;
            }

            String statName = matcher.group(1).trim();
            if (statName.equalsIgnoreCase("Mana Cost")) {
                continue;
            }

            try {
                result.put(
                    statName,
                    Double.parseDouble(matcher.group(2))
                );
            } catch (NumberFormatException ignored) {
            }
        }

        return result;
    }

    private static String formatDelta(String name, double delta) {
        String number = Math.abs(delta) >= 100 || Math.abs(delta) == Math.rint(Math.abs(delta))
            ? String.format("%.0f", Math.abs(delta))
            : String.format("%.1f", Math.abs(delta));

        return (delta > 0 ? "+" : "-") + " " + name + ": " + number;
    }
}
