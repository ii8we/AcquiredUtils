package dev.ii8we.acquiredutils.client.features;

import dev.ii8we.acquiredutils.AcquiredUtils;
import dev.ii8we.acquiredutils.client.playerclass.PlayerHudDataReader;
import dev.ii8we.acquiredutils.client.compat.ServerCompatibility;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

/** Renders custom health and mana bars using the server actionbar HUD values. */
public final class HealthManaBarOverlay {
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(AcquiredUtils.MOD_ID, "health_mana_bars");
    private static final int BAR_WIDTH = 150;
    private static final int BAR_HEIGHT = 12;

    private HealthManaBarOverlay() {}

    public static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, HUD_ID, HealthManaBarOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();
        if (client.player == null || client.options.hideGui || client.screen != null) return;

        if (ServerCompatibility.isFeatureAllowed("health_bar_overlay")) {
            renderBar(graphics, client, cfg.healthBarOverlayEnabled, cfg.healthBarOverlayPositionX, cfg.healthBarOverlayPositionY, cfg.healthBarOverlayScale,
                PlayerHudDataReader.getHealthCurrent(), PlayerHudDataReader.getHealthMax(), "Health", 0xFFE84B5F, 0xFF7D2430);
        }
        if (ServerCompatibility.isFeatureAllowed("mana_bar_overlay")) {
            renderBar(graphics, client, cfg.manaBarOverlayEnabled, cfg.manaBarOverlayPositionX, cfg.manaBarOverlayPositionY, cfg.manaBarOverlayScale,
                PlayerHudDataReader.getManaCurrent(), PlayerHudDataReader.getManaMax(), "Mana", 0xFF42D9F5, 0xFF15536A);
        }
    }

    private static void renderBar(GuiGraphics graphics, Minecraft client, boolean enabled, float px, float py, float scale,
                                  int current, int max, String label, int fillColor, int emptyColor) {
        if (!enabled || max <= 0 || current < 0) return;
        scale = Float.isFinite(scale) && scale > 0 ? scale : 1.0f;
        int width = Math.round(BAR_WIDTH * scale);
        int height = Math.round(BAR_HEIGHT * scale);
        int x = Math.round(px * client.getWindow().getGuiScaledWidth() - width / 2.0f);
        int y = Math.round(py * client.getWindow().getGuiScaledHeight());
        x = Math.max(2, Math.min(x, client.getWindow().getGuiScaledWidth() - width - 2));
        y = Math.max(2, Math.min(y, client.getWindow().getGuiScaledHeight() - height - 2));

        graphics.fill(x, y, x + width, y + height, 0xB0000000);
        graphics.fill(x + Math.round(scale), y + Math.round(scale), x + width - Math.round(scale), y + height - Math.round(scale), emptyColor);
        int innerWidth = Math.max(1, width - Math.round(scale * 2));
        int fillWidth = Math.round(innerWidth * Math.min(1.0f, current / (float) max));
        graphics.fill(x + Math.round(scale), y + Math.round(scale), x + Math.round(scale) + fillWidth, y + height - Math.round(scale), fillColor);
        String text = label + " " + current + "/" + max;
        int textWidth = client.font.width(text);
        graphics.drawString(client.font, text, x + (width - textWidth) / 2, y + Math.max(1, (height - client.font.lineHeight) / 2), 0xFFFFFFFF, true);
    }
}
