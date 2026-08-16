package dev.ii8we.acquiredutils.client.features;

import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FeatureRegistry {
    private static final Map<String, ClientFeature> FEATURES = new LinkedHashMap<>();
    private static boolean initialized;

    private FeatureRegistry() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        register(new ConfigFeature("item_position", () -> AcquiredUtilsConfig.get().itemPositionEnabled, value -> AcquiredUtilsConfig.get().itemPositionEnabled = value, () -> {
            AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();
            cfg.itemPositionEnabled = true;
            cfg.mainHandPositionX = cfg.mainHandPositionY = cfg.mainHandPositionZ = 0.0f;
            cfg.offhandPositionX = cfg.offhandPositionY = cfg.offhandPositionZ = 0.0f;
            cfg.mainHandRotationX = cfg.mainHandRotationY = cfg.mainHandRotationZ = 0.0f;
            cfg.offhandRotationX = cfg.offhandRotationY = cfg.offhandRotationZ = 0.0f;
            cfg.mainHandScale = cfg.offhandScale = 1.0f;
        }));
        register(new ConfigFeature("item_pickup_notifier", () -> AcquiredUtilsConfig.get().itemPickupNotifierEnabled, value -> AcquiredUtilsConfig.get().itemPickupNotifierEnabled = value, () -> {
            AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();
            cfg.itemPickupNotifierEnabled = true;
            cfg.notificationDuration = 3.0f;
            cfg.notificationPositionX = 0.5f;
            cfg.notificationPositionY = 0.25f;
        }));
        register(new ConfigFeature("rarity_highlight", () -> AcquiredUtilsConfig.get().rarityCircleEnabled, value -> AcquiredUtilsConfig.get().rarityCircleEnabled = value, () -> AcquiredUtilsConfig.get().rarityCircleEnabled = true));
        register(new ConfigFeature("slot_lock", () -> AcquiredUtilsConfig.get().slotLockEnabled, value -> {
            AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();
            cfg.slotLockEnabled = value;
            if (value && cfg.slotLockKey < 0) {
                cfg.slotLockKey = com.mojang.blaze3d.platform.InputConstants.KEY_Z;
            }
        }, () -> {
            AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();
            cfg.slotLockEnabled = false;
            cfg.lockedSlots.clear();
        }));
        register(new ConfigFeature("inventory_search", () -> AcquiredUtilsConfig.get().inventorySearchEnabled, value -> AcquiredUtilsConfig.get().inventorySearchEnabled = value, () -> AcquiredUtilsConfig.get().inventorySearchEnabled = true));
        register(new ConfigFeature("inventory_full_warning", () -> AcquiredUtilsConfig.get().inventoryFullWarningEnabled, value -> AcquiredUtilsConfig.get().inventoryFullWarningEnabled = value, () -> AcquiredUtilsConfig.get().inventoryFullWarningEnabled = true));
        register(new ConfigFeature("gear_comparison", () -> AcquiredUtilsConfig.get().itemComparisonEnabled, value -> AcquiredUtilsConfig.get().itemComparisonEnabled = value, () -> AcquiredUtilsConfig.get().itemComparisonEnabled = true));
        register(new ConfigFeature("recipe_unlock_highlight", () -> AcquiredUtilsConfig.get().recipeUnlockHighlightEnabled, value -> AcquiredUtilsConfig.get().recipeUnlockHighlightEnabled = value, () -> AcquiredUtilsConfig.get().recipeUnlockHighlightEnabled = true));
    }

    public static void register(ClientFeature feature) {
        if (feature == null || feature.id() == null || feature.id().isBlank()) return;
        FEATURES.putIfAbsent(feature.id(), feature);
    }

    public static ClientFeature get(String id) {
        init();
        return FEATURES.get(id);
    }

    public static Collection<ClientFeature> all() {
        init();
        return FEATURES.values();
    }

    public static void reset(String id) {
        ClientFeature feature = get(id);
        if (feature == null) return;
        feature.reset();
        AcquiredUtilsConfig.get().markDirty();
        AcquiredUtilsConfig.save();
    }

    private record ConfigFeature(String id, java.util.function.BooleanSupplier getter, java.util.function.Consumer<Boolean> setter, Runnable resetAction) implements ClientFeature {
        @Override public boolean isEnabled() { return getter.getAsBoolean(); }
        @Override public void setEnabled(boolean enabled) { setter.accept(enabled); }
        @Override public void reset() { resetAction.run(); }
    }
}
