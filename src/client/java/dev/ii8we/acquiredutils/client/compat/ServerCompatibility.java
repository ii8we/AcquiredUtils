package dev.ii8we.acquiredutils.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Locale;
import java.util.Set;

public final class ServerCompatibility {
    private static final String SERVER_KEYWORD = "fakepixel";
    private static final Set<String> SERVER_FEATURES = Set.of(
        "rarity_highlight",
        "rarity_glint",
        "inventory_search",
        "gear_comparison",
        "recipe_unlock_highlight",
        "health_bar_overlay",
        "mana_bar_overlay",
        "player_abilities_panel"
    );

    private ServerCompatibility() {
    }

    public static boolean isFeatureAllowed(String featureId) {
        return !SERVER_FEATURES.contains(featureId) || isTargetServer();
    }

    public static boolean isTargetServer() {
        Minecraft client = Minecraft.getInstance();
        ServerData server = client.getCurrentServer();
        if (server == null || server.ip == null) {
            return false;
        }
        return server.ip.toLowerCase(Locale.ROOT).contains(SERVER_KEYWORD);
    }
}
