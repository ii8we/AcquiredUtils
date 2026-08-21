package dev.ii8we.acquiredutils.client.playerclass;

import java.util.ArrayList;
import java.util.List;

public final class PlayerClassData {
    public String playerClass;
    /** Runtime-only HUD data. This value is never serialized to local class files. */
    public transient String activePet;
    public List<PlayerAbilityData> abilities;

    public PlayerClassData() {
        this.playerClass = "Warrior";
        this.activePet = "";
        this.abilities = new ArrayList<>();
    }

    public static PlayerClassData defaults(PlayerClass playerClass) {
        PlayerClassData data = new PlayerClassData();
        data.playerClass = playerClass.displayName();
        data.activePet = "";
        data.abilities = new ArrayList<>();
        return data;
    }

    public void sanitize(PlayerClass expectedClass) {
        if (playerClass == null || playerClass.isBlank()) {
            playerClass = expectedClass.displayName();
        }
        if (activePet == null) {
            activePet = "";
        }
        if (abilities == null) {
            abilities = new ArrayList<>();
        }
        abilities.removeIf(ability -> ability == null);
        for (PlayerAbilityData ability : abilities) {
            ability.sanitize();
        }
    }
}
