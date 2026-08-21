package dev.ii8we.acquiredutils.client.playerclass;

/** Local class ability data. */
public final class PlayerAbilityData {
    public String name;
    public String clicks;

    public PlayerAbilityData() {
        this.name = "";
        this.clicks = "Not set";
    }

    public PlayerAbilityData(String name, String clicks) {
        this.name = name == null ? "" : name;
        this.clicks = clicks == null || clicks.isBlank() ? "Not set" : clicks;
    }

    public void sanitize() {
        if (name == null) name = "";
        if (clicks == null || clicks.isBlank()) clicks = "Not set";
    }
}
