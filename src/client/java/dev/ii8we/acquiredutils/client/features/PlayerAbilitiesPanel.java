package dev.ii8we.acquiredutils.client.features;

import dev.ii8we.acquiredutils.AcquiredUtils;
import dev.ii8we.acquiredutils.client.playerclass.PlayerAbilityData;
import dev.ii8we.acquiredutils.client.playerclass.PlayerClassData;
import dev.ii8we.acquiredutils.client.playerclass.PlayerClassDataManager;
import dev.ii8we.acquiredutils.client.compat.ServerCompatibility;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Player Profile HUD panel shown outside inventory screens. */
public final class PlayerAbilitiesPanel {
    private static final Identifier HUD_ID =
        Identifier.fromNamespaceAndPath(AcquiredUtils.MOD_ID, "player_profile_panel");

    private static final int PANEL_WIDTH = 150;
    private static final int PANEL_HEIGHT = 148;
    private static final int PADDING = 7;
    private static final float TEXT_SCALE = 0.78f;

    private PlayerAbilitiesPanel() {}

    public static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, HUD_ID, PlayerAbilitiesPanel::renderHud);
        AcquiredUtils.LOGGER.info("[AcquiredUtils] Player profile HUD panel initialized");
    }

    private static void renderHud(GuiGraphics graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();
        if (!ServerCompatibility.isFeatureAllowed("player_abilities_panel") || !cfg.playerAbilitiesPanelEnabled || client.player == null || client.options.hideGui || client.screen != null) {
            return;
        }

        float scale = cfg.playerAbilitiesPanelScale;
        if (!Float.isFinite(scale) || scale <= 0.0f) scale = 1.0f;

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int panelWidth = Math.round(PANEL_WIDTH * scale);
        int panelHeight = Math.round(PANEL_HEIGHT * scale);

        int baseX = Math.round(cfg.playerAbilitiesPanelPositionX * screenWidth);
        int baseY = Math.round(cfg.playerAbilitiesPanelPositionY * screenHeight);
        baseX = Math.max(4, Math.min(baseX, screenWidth - panelWidth - 4));
        baseY = Math.max(4, Math.min(baseY, screenHeight - panelHeight - 4));

        PlayerClassData data = PlayerClassDataManager.getActiveData();
        graphics.pose().pushMatrix();
        graphics.pose().translate(baseX, baseY);
        graphics.pose().scale(scale, scale);
        drawPanel(graphics, client, data);
        graphics.pose().popMatrix();
    }

    private static void drawPanel(GuiGraphics graphics, Minecraft client, PlayerClassData data) {
        graphics.fill(2, 2, PANEL_WIDTH + 2, PANEL_HEIGHT + 2, 0x40000000);
        graphics.fill(0, 0, PANEL_WIDTH, PANEL_HEIGHT, 0xC8101014);
        graphics.fill(0, 0, PANEL_WIDTH, 1, 0xD04C5360);
        graphics.fill(0, PANEL_HEIGHT - 1, PANEL_WIDTH, PANEL_HEIGHT, 0xC02A2E35);
        graphics.fill(0, 0, 1, PANEL_HEIGHT, 0xC02A2E35);
        graphics.fill(PANEL_WIDTH - 1, 0, PANEL_WIDTH, PANEL_HEIGHT, 0xC02A2E35);

        drawCentered(graphics, client, Component.translatable("acquiredutils.overlay.player_profile.title"), 6, 0xFFFFFFFF);
        graphics.fill(PADDING, 20, PANEL_WIDTH - PADDING, 21, 0xFF30353D);

        drawKeyValue(graphics, client, "Class", data.playerClass, 28);
        String pet = data.activePet == null || data.activePet.isBlank() ? "None" : data.activePet;
        drawKeyValue(graphics, client, "Active Pet", pet, 43);

        graphics.fill(PADDING, 58, PANEL_WIDTH - PADDING, 59, 0xFF30353D);
        drawString(graphics, client, Component.literal("Abilities"), PADDING, 64, 0xFFFFFFFF, true);

        int y = 76;
        if (data.abilities.isEmpty()) {
            String message = "Open your " + data.playerClass + " Ability Tree";
            String message2 = "vault once to load abilities.";
            drawString(graphics, client, Component.literal(message), PADDING, y, 0xFFB8BDC7, false);
            drawString(graphics, client, Component.literal(message2), PADDING, y + 10, 0xFFB8BDC7, false);
        } else {
            for (PlayerAbilityData ability : data.abilities) {
                if (y > PANEL_HEIGHT - 12) break;
                String name = ability.name == null || ability.name.isBlank() ? "Unknown" : ability.name;
                String clicks = compactClicks(ability.clicks);
                drawString(graphics, client, Component.literal("• " + name + " [" + clicks + "]"), PADDING, y, 0xFFD8DCE3, false);
                y += 10;
            }
        }
    }

    private static String compactClicks(String clicks) {
        if (clicks == null || clicks.isBlank()) return "Not set";
        return clicks.replace("LEFT", "L").replace("RIGHT", "R");
    }

    private static void drawKeyValue(GuiGraphics graphics, Minecraft client, String label, String value, int y) {
        drawString(graphics, client, Component.literal(label), PADDING, y, 0xFF8F98A6, false);
        int valueWidth = Math.round(client.font.width(value) * TEXT_SCALE);
        int x = PANEL_WIDTH - PADDING - valueWidth;
        drawString(graphics, client, Component.literal(value), Math.max(PADDING + 42, x), y, 0xFFFFFFFF, false);
    }

    private static void drawString(GuiGraphics graphics, Minecraft client, Component text, int x, int y, int color, boolean shadow) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(TEXT_SCALE, TEXT_SCALE);
        graphics.drawString(client.font, text, 0, 0, color, shadow);
        graphics.pose().popMatrix();
    }

    private static void drawCentered(GuiGraphics graphics, Minecraft client, Component text, int y, int color) {
        int textWidth = Math.round(client.font.width(text) * TEXT_SCALE);
        drawString(graphics, client, text, Math.max(0, (PANEL_WIDTH - textWidth) / 2), y, color, true);
    }
}
