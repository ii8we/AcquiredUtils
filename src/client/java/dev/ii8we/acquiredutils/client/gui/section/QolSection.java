package dev.ii8we.acquiredutils.client.gui.section;

import dev.ii8we.acquiredutils.client.gui.AcquiredUtilsConfigScreen;
import dev.ii8we.acquiredutils.client.gui.PickupHudEditorScreen;
import dev.ii8we.acquiredutils.client.gui.widget.FeatureControlWidget;
import dev.ii8we.acquiredutils.client.gui.widget.ThemedButtonWidget;
import dev.ii8we.acquiredutils.client.gui.widget.ValueSliderWidget;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class QolSection extends ModSection {

    public QolSection(AcquiredUtilsConfigScreen screen) {
        super(screen);
    }

    @Override
    public String getId() {
        return "qol";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("acquiredutils.gui.tab.qol");
    }

    @Override
    public List<GuiFeature> getFeatures() {
        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();

        return List.of(
            new GuiFeature(
                "inventory_search",
                "acquiredutils.gui.setting.search_inventory",
                "acquiredutils.gui.desc.search_inventory",
                () -> cfg.inventorySearchEnabled,
                enabled -> {
                    cfg.inventorySearchEnabled = enabled;
                    cfg.markDirty();
                    screen.scheduleRebuild();
                },
                (x, y, w, h) -> new FeatureControlWidget(x, y, w, h, "inventory_search", screen::scheduleRebuild),
                List.of()
            ),
            new GuiFeature(
                "inventory_full_warning",
                "acquiredutils.gui.setting.inventory_full_warning",
                "acquiredutils.gui.desc.inventory_full_warning",
                () -> cfg.inventoryFullWarningEnabled,
                enabled -> {
                    cfg.inventoryFullWarningEnabled = enabled;
                    cfg.markDirty();
                    screen.scheduleRebuild();
                },
                (x, y, w, h) -> new FeatureControlWidget(x, y, w, h, "inventory_full_warning", screen::scheduleRebuild),
                List.of()
            ),
            new GuiFeature(
                "slot_lock",
                "acquiredutils.gui.setting.slot_lock",
                "acquiredutils.gui.desc.slot_lock",
                () -> cfg.slotLockEnabled,
                enabled -> {
                    cfg.slotLockEnabled = enabled;
                    if (enabled && cfg.slotLockKey < 0) {
                        cfg.slotLockKey = com.mojang.blaze3d.platform.InputConstants.KEY_Z;
                    }
                    cfg.markDirty();
                    AcquiredUtilsConfig.saveIfDirty();
                    screen.scheduleRebuild();
                },
                (x, y, w, h) -> new FeatureControlWidget(x, y, w, h, "slot_lock", screen::scheduleRebuild),
                List.of()
            ),
            new GuiFeature(
                "gear_comparison",
                "acquiredutils.gui.setting.item_comparison",
                "acquiredutils.gui.desc.item_comparison",
                () -> cfg.itemComparisonEnabled,
                enabled -> {
                    cfg.itemComparisonEnabled = enabled;
                    cfg.markDirty();
                    screen.scheduleRebuild();
                },
                (x, y, w, h) -> new FeatureControlWidget(x, y, w, h, "gear_comparison", screen::scheduleRebuild),
                List.of()
            ),
            new GuiFeature(
                "item_pickup_notifier",
                "acquiredutils.gui.setting.item_pickup_notifier",
                "acquiredutils.gui.desc.item_pickup_notifier",
                () -> cfg.itemPickupNotifierEnabled,
                enabled -> {
                    cfg.itemPickupNotifierEnabled = enabled;
                    cfg.markDirty();
                    screen.scheduleRebuild();
                },
                (x, y, w, h) -> new FeatureControlWidget(x, y, w, h, "item_pickup_notifier", screen::scheduleRebuild),
                List.of(
                    new GuiRow(
                        "acquiredutils.gui.setting.notification_duration",
                        "acquiredutils.gui.desc.notification_duration",
                        22, 150, 40, 48,
                        (x, y, w, h) -> new ValueSliderWidget(
                            x, y, w, h,
                            cfg.notificationDuration,
                            3.0f, 0.5f, 10.0f, 0.5f,
                            false, "s",
                            value -> { cfg.notificationDuration = value; cfg.markDirty(); }
                        )
                    ),
                    new GuiRow(
                        "acquiredutils.gui.setting.edit_hud",
                        "acquiredutils.gui.desc.edit_hud",
                        22, 150, 20, 48,
                        (x, y, w, h) -> new ThemedButtonWidget(
                            x, y, w, h,
                            Component.translatable("acquiredutils.gui.button.edit_hud"),
                            () -> screen.getMinecraft().setScreen(new PickupHudEditorScreen(screen))
                        )
                    )
                )
            ),
            new GuiFeature(
                "item_position",
                "acquiredutils.gui.setting.item_position",
                "acquiredutils.gui.desc.item_position",
                () -> cfg.itemPositionEnabled,
                enabled -> {
                    cfg.itemPositionEnabled = enabled;
                    cfg.markDirty();
                    screen.scheduleRebuild();
                },
                (x, y, w, h) -> new FeatureControlWidget(x, y, w, h, "item_position", screen::scheduleRebuild),
                List.of(
                    positionRow("acquiredutils.gui.setting.main_hand_x", "acquiredutils.gui.desc.main_hand_x", cfg.mainHandPositionX, value -> cfg.mainHandPositionX = value),
                    positionRow("acquiredutils.gui.setting.main_hand_y", "acquiredutils.gui.desc.main_hand_y", cfg.mainHandPositionY, value -> cfg.mainHandPositionY = value),
                    positionRow("acquiredutils.gui.setting.main_hand_z", "acquiredutils.gui.desc.main_hand_z", cfg.mainHandPositionZ, value -> cfg.mainHandPositionZ = value),
                    positionRow("acquiredutils.gui.setting.offhand_x", "acquiredutils.gui.desc.offhand_x", cfg.offhandPositionX, value -> cfg.offhandPositionX = value),
                    positionRow("acquiredutils.gui.setting.offhand_y", "acquiredutils.gui.desc.offhand_y", cfg.offhandPositionY, value -> cfg.offhandPositionY = value),
                    positionRow("acquiredutils.gui.setting.offhand_z", "acquiredutils.gui.desc.offhand_z", cfg.offhandPositionZ, value -> cfg.offhandPositionZ = value),
                    angleRow("acquiredutils.gui.setting.main_hand_rot_x", "acquiredutils.gui.desc.main_hand_rot_x", cfg.mainHandRotationX, value -> cfg.mainHandRotationX = value),
                    angleRow("acquiredutils.gui.setting.main_hand_rot_y", "acquiredutils.gui.desc.main_hand_rot_y", cfg.mainHandRotationY, value -> cfg.mainHandRotationY = value),
                    angleRow("acquiredutils.gui.setting.main_hand_rot_z", "acquiredutils.gui.desc.main_hand_rot_z", cfg.mainHandRotationZ, value -> cfg.mainHandRotationZ = value),
                    angleRow("acquiredutils.gui.setting.offhand_rot_x", "acquiredutils.gui.desc.offhand_rot_x", cfg.offhandRotationX, value -> cfg.offhandRotationX = value),
                    angleRow("acquiredutils.gui.setting.offhand_rot_y", "acquiredutils.gui.desc.offhand_rot_y", cfg.offhandRotationY, value -> cfg.offhandRotationY = value),
                    angleRow("acquiredutils.gui.setting.offhand_rot_z", "acquiredutils.gui.desc.offhand_rot_z", cfg.offhandRotationZ, value -> cfg.offhandRotationZ = value),
                    scaleRow("acquiredutils.gui.setting.main_hand_scale", "acquiredutils.gui.desc.main_hand_scale", cfg.mainHandScale, value -> cfg.mainHandScale = value),
                    scaleRow("acquiredutils.gui.setting.offhand_scale", "acquiredutils.gui.desc.offhand_scale", cfg.offhandScale, value -> cfg.offhandScale = value)
                )
            ),
            new GuiFeature(
                "recipe_unlock_highlight",
                "acquiredutils.gui.setting.recipe_unlock_highlight",
                "acquiredutils.gui.desc.recipe_unlock_highlight",
                () -> cfg.recipeUnlockHighlightEnabled,
                enabled -> {
                    cfg.recipeUnlockHighlightEnabled = enabled;
                    cfg.markDirty();
                    screen.scheduleRebuild();
                },
                (x, y, w, h) -> new FeatureControlWidget(x, y, w, h, "recipe_unlock_highlight", screen::scheduleRebuild),
                List.of()
            )
        );
    }

    private GuiRow angleRow(String labelKey, String descKey, float current, java.util.function.Consumer<Float> setter) {
        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();
        return new GuiRow(labelKey, descKey, 22, 150, 40, 48, (x, y, w, h) -> new ValueSliderWidget(
            x, y, w, h, current, 0.0f, -180.0f, 180.0f, 1.0f, false, "°",
            value -> { setter.accept(value); cfg.markDirty(); }
        ));
    }

    private GuiRow scaleRow(String labelKey, String descKey, float current, java.util.function.Consumer<Float> setter) {
        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();
        return new GuiRow(labelKey, descKey, 22, 150, 40, 48, (x, y, w, h) -> new ValueSliderWidget(
            x, y, w, h, current, 1.0f, 0.25f, 2.0f, 0.05f, false, "x",
            value -> { setter.accept(value); cfg.markDirty(); }
        ));
    }

    private GuiRow positionRow(String labelKey, String descKey, float current, java.util.function.Consumer<Float> setter) {
        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();
        return new GuiRow(
            labelKey, descKey, 22, 150, 40, 48,
            (x, y, w, h) -> new ValueSliderWidget(
                x, y, w, h, current, 0.0f, -1.0f, 1.0f, 0.01f, false, "",
                value -> { setter.accept(value); cfg.markDirty(); }
            )
        );
    }


}
