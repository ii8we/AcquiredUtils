package dev.bobodado.acquiredutils.client.pickup;


public enum ItemRarity {
    COMMON(0xFFAAAAAA),
    UNCOMMON(0xFF55FF55),
    RARE(0xFF5555FF),
    EPIC(0xFFAA00AA),
    LEGENDARY(0xFFFFAA00),
    MYTHIC(0xFFFF55FF);

    private final int color;

    ItemRarity(int color) {
        this.color = color;
    }

    public int color() {
        return color;
    }


    public static ItemRarity fromName(String name) {
        if (name == null) return COMMON;
        try {
            return valueOf(name.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return COMMON;
        }
    }

    public static ItemRarity fromColor(Integer color) {
        if (color == null) return null;
        int rgb = color & 0xFFFFFF;

        if (rgb == 0xAAAAAA) return COMMON;
        if (rgb == 0x55FF55) return UNCOMMON;
        if (rgb == 0x5555FF) return RARE;
        if (rgb == 0xAA00AA) return EPIC;
        if (rgb == 0xFFAA00) return LEGENDARY;
        if (rgb == 0xFF55FF) return MYTHIC;
        return null;
    }
}
