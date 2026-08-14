package dev.bobodado.acquiredutils.client.gui.section;

import dev.bobodado.acquiredutils.client.gui.AcquiredUtilsConfigScreen;
import dev.bobodado.acquiredutils.client.gui.widget.ValueSliderWidget;
import dev.bobodado.acquiredutils.client.gui.widget.ThemedCheckboxWidget;
import dev.bobodado.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.network.chat.Component;

import java.util.List;

public class OverlaysSection extends ModSection {

    public OverlaysSection(AcquiredUtilsConfigScreen screen) {
        super(screen);
    }

    @Override
    public String getId() {
        return "overlays";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("acquiredutils.gui.tab.overlays");
    }

    @Override
    public List<GuiRow> getRows() {
        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();

        return List.of(
            new GuiRow(
                "acquiredutils.gui.setting.recipe_unlock_highlight",
                "acquiredutils.gui.desc.recipe_unlock_highlight",
                22, 20, 20, 48,
                (x, y, w, h) -> new ThemedCheckboxWidget(
                    x + w - s(24), y + s(2), s(20), s(18),
                    () -> cfg.recipeUnlockHighlightEnabled,
                    checked -> { cfg.recipeUnlockHighlightEnabled = checked; cfg.markDirty(); }
                )
            ),
            new GuiRow(
                "acquiredutils.gui.setting.rarity_circle",
                "acquiredutils.gui.desc.rarity_circle",
                22, 20, 20, 48,
                (x, y, w, h) -> new ThemedCheckboxWidget(
                    x + w - s(24), y + s(2), s(20), s(18),
                    () -> cfg.rarityCircleEnabled,
                    checked -> { cfg.rarityCircleEnabled = checked; cfg.markDirty(); }
                )
            ),
            new GuiRow(
                "acquiredutils.gui.setting.rarity_circle_size",
                "acquiredutils.gui.desc.rarity_circle_size",
                22, 150, 20, 48,
                (x, y, w, h) -> new ValueSliderWidget(
                    x, y, w, h,
                    cfg.rarityCircleSize,
                    3.0f,
                    8.0f,
                    1.0f,
                    false,
                    "px",
                    value -> {
                        cfg.rarityCircleSize = value;
                        cfg.markDirty();
                    }
                )
            ),
            new GuiRow(
                "acquiredutils.gui.setting.rarity_circle_opacity",
                "acquiredutils.gui.desc.rarity_circle_opacity",
                22, 150, 20, 48,
                (x, y, w, h) -> new ValueSliderWidget(
                    x, y, w, h,
                    cfg.rarityCircleOpacity,
                    0.15f,
                    1.0f,
                    0.05f,
                    false,
                    "",
                    value -> {
                        cfg.rarityCircleOpacity = value;
                        cfg.markDirty();
                    }
                )
            ),
            new GuiRow(
                "acquiredutils.gui.setting.item_comparison",
                "acquiredutils.gui.desc.item_comparison",
                22, 20, 20, 48,
                (x, y, w, h) -> new ThemedCheckboxWidget(
                    x + w - s(24), y + s(2), s(20), s(18),
                    () -> cfg.itemComparisonEnabled,
                    checked -> { cfg.itemComparisonEnabled = checked; cfg.markDirty(); }
                )
            )
        );
    }
}
