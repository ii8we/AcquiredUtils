package dev.ii8we.acquiredutils.client.playerclass;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads class data from the client-visible sidebar and pet/level data from the tab list. */
public final class PlayerHudDataReader {
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static int tickCounter;
    private static int healthCurrent;
    private static int healthMax;
    private static int manaCurrent;
    private static int manaMax;
    private static Component lastOverlayMessage;
    private static final Pattern HEALTH_PATTERN = Pattern.compile("(?i)(?:[♥❤]|health)[^0-9]*([0-9]+)\\s*/\\s*([0-9]+)");
    private static final Pattern MANA_PATTERN = Pattern.compile("(?i)focus\\s*:?\\s*([0-9]+)\\s*/\\s*([0-9]+)");

    private PlayerHudDataReader() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) {
                lastOverlayMessage = message.copy();
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (++tickCounter < SCAN_INTERVAL_TICKS) return;
            tickCounter = 0;
            scan(client);
        });
    }

    private static void scan(Minecraft client) {
        if (client.player == null || client.level == null || client.getConnection() == null) return;

        readHealthAndMana(client);

        String detectedClass = detectClassFromSidebar(client);
        String detectedPet = null;

        for (PlayerInfo info : client.getConnection().getOnlinePlayers()) {
            String display = info.getTabListDisplayName() == null ? "" : info.getTabListDisplayName().getString();
            String profile = info.getProfile().name();

            String combined = display + "\n" + profile;
            for (String rawLine : combined.split("\\R")) {
                String line = stripFormatting(rawLine).trim();
                if (line.isEmpty()) continue;

                String petValue = valueAfterLabel(line, "Pet");
                if (petValue != null) detectedPet = petValue;

            }
        }

        if (detectedClass == null) {
            detectedClass = detectClassFromTabList(client);
        }

        PlayerClass activeClass = PlayerClass.fromDisplayName(detectedClass);
        if (activeClass == null) activeClass = PlayerClassDataManager.getActiveClass();
        if (activeClass == null) return;

        if (activeClass != PlayerClassDataManager.getActiveClass()) {
            PlayerClassDataManager.setActiveClass(activeClass);
        }

        PlayerClassData data = PlayerClassDataManager.getData(activeClass);
        boolean changed = false;
        if (!activeClass.displayName().equals(data.playerClass)) {
            data.playerClass = activeClass.displayName();
            changed = true;
        }
        if (detectedPet != null && !detectedPet.equals(data.activePet)) {
            data.activePet = detectedPet;
        }
        if (changed) PlayerClassDataManager.saveClass(activeClass);
    }


    private static void readHealthAndMana(Minecraft client) {
        if (lastOverlayMessage == null) {
            healthCurrent = healthMax = manaCurrent = manaMax = 0;
            return;
        }
        String text = stripFormatting(lastOverlayMessage.getString()).replaceAll("\\s+", " ").trim();

        Matcher manaMatcher = MANA_PATTERN.matcher(text);
        if (manaMatcher.find()) {
            int current = parseInt(manaMatcher.group(1));
            int max = parseInt(manaMatcher.group(2));
            if (isValidPair(current, max)) {
                manaCurrent = current;
                manaMax = max;
            }
        }

        Matcher healthMatcher = HEALTH_PATTERN.matcher(text);
        if (healthMatcher.find()) {
            int current = parseInt(healthMatcher.group(1));
            int max = parseInt(healthMatcher.group(2));
            // The first current/max pair in this actionbar is the custom health value.
            if (isValidPair(current, max)) {
                healthCurrent = current;
                healthMax = max;
            }
        }
    }

    private static int parseInt(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return 0; }
    }

    private static boolean isValidPair(int current, int max) {
        return current >= 0 && max > 0 && current <= max;
    }

    public static int getHealthCurrent() { return healthCurrent; }
    public static int getHealthMax() { return healthMax; }
    public static int getManaCurrent() { return manaCurrent; }
    public static int getManaMax() { return manaMax; }

    private static String detectClassFromSidebar(Minecraft client) {
        Scoreboard scoreboard = client.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return null;

        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            if (entry.isHidden()) continue;

            String line = scoreboardLine(scoreboard, entry);
            String classValue = valueAfterLabel(stripFormatting(line), "Class");
            PlayerClass playerClass = PlayerClass.fromDisplayName(classValue);
            if (playerClass != null) return playerClass.displayName();
        }
        return null;
    }

    private static String scoreboardLine(Scoreboard scoreboard, PlayerScoreEntry entry) {
        Component display = entry.display();
        if (display != null) {
            String text = display.getString();
            if (!text.isBlank()) return text;
        }

        Component owner = Component.literal(entry.owner());
        PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
        if (team != null) {
            owner = PlayerTeam.formatNameForTeam(team, owner);
        }
        return owner.getString();
    }

    private static String detectClassFromTabList(Minecraft client) {
        for (PlayerInfo info : client.getConnection().getOnlinePlayers()) {
            String display = info.getTabListDisplayName() == null ? "" : info.getTabListDisplayName().getString();
            String profile = info.getProfile().name();
            String combined = display + "\n" + profile;
            for (String rawLine : combined.split("\\R")) {
                String line = stripFormatting(rawLine).trim();
                if (line.isEmpty()) continue;

                String classValue = valueAfterLabel(line, "Class");
                if (classValue == null) classValue = valueAfterLabel(line, "Profile");
                if (PlayerClass.fromDisplayName(classValue) != null) {
                    return PlayerClass.fromDisplayName(classValue).displayName();
                }
            }
        }
        return null;
    }

    private static String valueAfterLabel(String line, String label) {
        if (line == null) return null;
        String lower = line.toLowerCase(Locale.ROOT);
        String target = label.toLowerCase(Locale.ROOT);
        int index = lower.indexOf(target);
        if (index < 0) return null;

        String remainder = line.substring(index + label.length()).trim();
        if (remainder.startsWith(":")) remainder = remainder.substring(1).trim();
        if (remainder.isEmpty()) return null;

        int separator = remainder.indexOf('|');
        if (separator >= 0) remainder = remainder.substring(0, separator).trim();
        return remainder;
    }

    private static String stripFormatting(String text) {
        return text.replaceAll("\\u00A7[0-9A-FK-ORa-fk-or]", "");
    }
}
