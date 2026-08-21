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

import java.util.ArrayList;
import java.util.List;
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
    private static int overlayAgeTicks = Integer.MAX_VALUE;
    private static final Pattern HEALTH_PATTERN = Pattern.compile("(?i)(?:[♥❤]|health)[^0-9]*([0-9]+)\\s*/\\s*([0-9]+)");
    private static final Pattern MANA_PATTERN = Pattern.compile("(?i)focus\\s*:?\\s*([0-9]+)\\s*/\\s*([0-9]+)");
    private static final Pattern PAIR_PATTERN = Pattern.compile("([0-9]+)\\s*/\\s*([0-9]+)");

    private PlayerHudDataReader() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) return;
            lastOverlayMessage = message.copy();
            overlayAgeTicks = 0;
            readHealthAndMana(message.getString());
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null || client.getConnection() == null) {
                resetHudValues();
                return;
            }
            if (overlayAgeTicks < Integer.MAX_VALUE) {
                overlayAgeTicks++;
                if (overlayAgeTicks > 40) resetHudValues();
            }
            if (++tickCounter < SCAN_INTERVAL_TICKS) return;
            tickCounter = 0;
            scan(client);
        });
    }

    private static void scan(Minecraft client) {
        if (client.player == null || client.level == null || client.getConnection() == null) return;

        String detectedClass = detectClassFromSidebar(client);
        String detectedPet = detectPetFromTabList(client);

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


    private static void readHealthAndMana(String rawText) {
        String text = stripFormatting(rawText).replaceAll("\\s+", " ").trim();
        int newHealthCurrent = 0;
        int newHealthMax = 0;
        int newManaCurrent = 0;
        int newManaMax = 0;

        Matcher healthMatcher = HEALTH_PATTERN.matcher(text);
        if (healthMatcher.find()) {
            int current = parseInt(healthMatcher.group(1));
            int max = parseInt(healthMatcher.group(2));
            if (isValidPair(current, max)) {
                newHealthCurrent = current;
                newHealthMax = max;
            }
        }

        Matcher manaMatcher = MANA_PATTERN.matcher(text);
        if (manaMatcher.find()) {
            int current = parseInt(manaMatcher.group(1));
            int max = parseInt(manaMatcher.group(2));
            if (isValidPair(current, max)) {
                newManaCurrent = current;
                newManaMax = max;
            }
        }

        List<int[]> pairs = new ArrayList<>();
        Matcher pairMatcher = PAIR_PATTERN.matcher(text);
        while (pairMatcher.find()) {
            int current = parseInt(pairMatcher.group(1));
            int max = parseInt(pairMatcher.group(2));
            if (isValidPair(current, max)) pairs.add(new int[] {current, max});
        }

        if (newHealthMax == 0 && !pairs.isEmpty()) {
            newHealthCurrent = pairs.get(0)[0];
            newHealthMax = pairs.get(0)[1];
        }
        if (newManaMax == 0 && pairs.size() > 1) {
            int[] pair = pairs.get(pairs.size() > 2 && newHealthMax > 0 ? 1 : pairs.size() - 1);
            newManaCurrent = pair[0];
            newManaMax = pair[1];
        }

        healthCurrent = newHealthCurrent;
        healthMax = newHealthMax;
        manaCurrent = newManaCurrent;
        manaMax = newManaMax;
    }

    private static void resetHudValues() {
        lastOverlayMessage = null;
        overlayAgeTicks = Integer.MAX_VALUE;
        healthCurrent = healthMax = manaCurrent = manaMax = 0;
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

    private static String detectPetFromTabList(Minecraft client) {
        PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
        if (info == null) return null;

        String display = info.getTabListDisplayName() == null ? "" : info.getTabListDisplayName().getString();
        String profile = info.getProfile().name();
        String combined = display + "\n" + profile;
        for (String rawLine : combined.split("\\R")) {
            String line = stripFormatting(rawLine).trim();
            if (line.isEmpty()) continue;
            String petValue = valueAfterLabel(line, "Pet");
            if (petValue != null) return petValue;
        }
        return null;
    }

    private static String detectClassFromTabList(Minecraft client) {
        PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
        if (info == null) return null;

        String display = info.getTabListDisplayName() == null ? "" : info.getTabListDisplayName().getString();
        String profile = info.getProfile().name();
        String combined = display + "\n" + profile;
        for (String rawLine : combined.split("\\R")) {
            String line = stripFormatting(rawLine).trim();
            if (line.isEmpty()) continue;

            String classValue = valueAfterLabel(line, "Class");
            if (classValue == null) classValue = valueAfterLabel(line, "Profile");
            PlayerClass playerClass = PlayerClass.fromDisplayName(classValue);
            if (playerClass != null) return playerClass.displayName();
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
