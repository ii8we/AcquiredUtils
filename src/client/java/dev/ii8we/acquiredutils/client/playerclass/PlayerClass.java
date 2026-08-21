package dev.ii8we.acquiredutils.client.playerclass;

public enum PlayerClass {
    WARRIOR("Warrior"),
    ROUGE("Rouge"),
    SAMURAI("Samurai"),
    ARCHER("Archer"),
    MAGE("Mage"),
    CLERIC("Cleric"),
    PALADIN("Paladin"),
    NECROMANCER("Necromancer"),
    DRUID("Druid"),
    BERSERKER("Berserker");

    private final String displayName;

    PlayerClass(String displayName) {
        this.displayName = displayName;
    }

    public static PlayerClass fromDisplayName(String name) {
        if (name == null) return null;
        for (PlayerClass playerClass : values()) {
            if (playerClass.displayName.equalsIgnoreCase(name.trim())) return playerClass;
        }
        return null;
    }

    public String displayName() {
        return displayName;
    }

    public String folderName() {
        return displayName.toLowerCase(java.util.Locale.ROOT);
    }
}
