package dev.ii8we.acquiredutils.client.gui.section;

import dev.ii8we.acquiredutils.client.gui.AcquiredUtilsConfigScreen;
import dev.ii8we.acquiredutils.client.gui.widget.FeatureControlWidget;
import dev.ii8we.acquiredutils.client.gui.widget.ValueSliderWidget;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class OverlaySection extends ModSection {

    public OverlaySection(AcquiredUtilsConfigScreen screen) {
        super(screen);
    }

    @Override
    public String getId() {
        return "overlay";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("acquiredutils.gui.tab.overlay");
    }

    @Override
    public List<GuiFeature> getFeatures() {
        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();

        return List.of(
            new GuiFeature(
                "rarity_highlight",
                "acquiredutils.gui.setting.rarity_circle",
                "acquiredutils.gui.desc.rarity_circle",
                () -> cfg.rarityCircleEnabled,
                enabled -> {
                    cfg.rarityCircleEnabled = enabled;
                    cfg.markDirty();
                    screen.scheduleRebuild();
                },
                (x, y, w, h) -> new FeatureControlWidget(x, y, w, h, "rarity_highlight", screen::scheduleRebuild),
                List.of()
            ),
            new GuiFeature(
                "rarity_glint",
                "acquiredutils.gui.setting.rarity_glint",
                "acquiredutils.gui.desc.rarity_glint",
                () -> cfg.rarityGlintEnabled,
                enabled -> {
                    cfg.rarityGlintEnabled = enabled;
                    cfg.markDirty();
                    screen.scheduleRebuild();
                },
                (x, y, w, h) -> new FeatureControlWidget(x, y, w, h, "rarity_glint", screen::scheduleRebuild),
                List.of()
            ),
            new GuiFeature(
                "player_abilities_panel",
                "acquiredutils.gui.setting.player_abilities_panel",
                "acquiredutils.gui.desc.player_abilities_panel",
                () -> cfg.playerAbilitiesPanelEnabled,
                enabled -> {
                    cfg.playerAbilitiesPanelEnabled = enabled;
                    cfg.markDirty();
                    screen.scheduleRebuild();
                },
                (x, y, w, h) -> new FeatureControlWidget(x, y, w, h, "player_abilities_panel", screen::scheduleRebuild),
                List.of(
                    new GuiRow(
                        "acquiredutils.gui.setting.player_abilities_panel_scale",
                        "acquiredutils.gui.desc.player_abilities_panel_scale",
                        22, 150, 40, 48,
                        (x, y, w, h) -> new ValueSliderWidget(
                            x, y, w, h,
                            cfg.playerAbilitiesPanelScale,
                            1.0f, 0.5f, 2.0f, 0.05f, false, "x",
                            value -> { cfg.playerAbilitiesPanelScale = value; cfg.markDirty(); }
                        )
                    ),
                    new GuiRow(
                        "acquiredutils.gui.setting.player_abilities_panel_x",
                        "acquiredutils.gui.desc.player_abilities_panel_x",
                        22, 150, 20, 48,
                        (x, y, w, h) -> new ValueSliderWidget(
                            x, y, w, h,
                            cfg.playerAbilitiesPanelPositionX,
                            0.72f, 0.0f, 1.0f, 0.01f, false, "",
                            value -> { cfg.playerAbilitiesPanelPositionX = value; cfg.markDirty(); }
                        )
                    ),
                    new GuiRow(
                        "acquiredutils.gui.setting.player_abilities_panel_y",
                        "acquiredutils.gui.desc.player_abilities_panel_y",
                        22, 150, 20, 48,
                        (x, y, w, h) -> new ValueSliderWidget(
                            x, y, w, h,
                            cfg.playerAbilitiesPanelPositionY,
                            0.20f, 0.0f, 1.0f, 0.01f, false, "",
                            value -> { cfg.playerAbilitiesPanelPositionY = value; cfg.markDirty(); }
                        )
                    )
                )
            ),
            new GuiFeature(
                "health_bar_overlay",
                "acquiredutils.gui.setting.health_bar_overlay",
                "acquiredutils.gui.desc.health_bar_overlay",
                () -> cfg.healthBarOverlayEnabled,
                enabled -> { cfg.healthBarOverlayEnabled = enabled; cfg.markDirty(); screen.scheduleRebuild(); },
                (x, y, w, h) -> new FeatureControlWidget(x, y, w, h, "health_bar_overlay", screen::scheduleRebuild),
                List.of(
                    new GuiRow("acquiredutils.gui.setting.health_bar_overlay_x", "acquiredutils.gui.desc.health_bar_overlay_x", 22, 150, 20, 48,
                        (x, y, w, h) -> new ValueSliderWidget(x, y, w, h, cfg.healthBarOverlayPositionX, 0.5f, 0.0f, 1.0f, 0.01f, false, "", v -> { cfg.healthBarOverlayPositionX = v; cfg.markDirty(); })),
                    new GuiRow("acquiredutils.gui.setting.health_bar_overlay_y", "acquiredutils.gui.desc.health_bar_overlay_y", 22, 150, 20, 48,
                        (x, y, w, h) -> new ValueSliderWidget(x, y, w, h, cfg.healthBarOverlayPositionY, 0.84f, 0.0f, 1.0f, 0.01f, false, "", v -> { cfg.healthBarOverlayPositionY = v; cfg.markDirty(); })),
                    new GuiRow("acquiredutils.gui.setting.health_bar_overlay_scale", "acquiredutils.gui.desc.health_bar_overlay_scale", 22, 150, 40, 48,
                        (x, y, w, h) -> new ValueSliderWidget(x, y, w, h, cfg.healthBarOverlayScale, 1.0f, 0.5f, 2.0f, 0.05f, false, "x", v -> { cfg.healthBarOverlayScale = v; cfg.markDirty(); }))
                )
            ),
            new GuiFeature(
                "mana_bar_overlay",
                "acquiredutils.gui.setting.mana_bar_overlay",
                "acquiredutils.gui.desc.mana_bar_overlay",
                () -> cfg.manaBarOverlayEnabled,
                enabled -> { cfg.manaBarOverlayEnabled = enabled; cfg.markDirty(); screen.scheduleRebuild(); },
                (x, y, w, h) -> new FeatureControlWidget(x, y, w, h, "mana_bar_overlay", screen::scheduleRebuild),
                List.of(
                    new GuiRow("acquiredutils.gui.setting.mana_bar_overlay_x", "acquiredutils.gui.desc.mana_bar_overlay_x", 22, 150, 20, 48,
                        (x, y, w, h) -> new ValueSliderWidget(x, y, w, h, cfg.manaBarOverlayPositionX, 0.5f, 0.0f, 1.0f, 0.01f, false, "", v -> { cfg.manaBarOverlayPositionX = v; cfg.markDirty(); })),
                    new GuiRow("acquiredutils.gui.setting.mana_bar_overlay_y", "acquiredutils.gui.desc.mana_bar_overlay_y", 22, 150, 20, 48,
                        (x, y, w, h) -> new ValueSliderWidget(x, y, w, h, cfg.manaBarOverlayPositionY, 0.89f, 0.0f, 1.0f, 0.01f, false, "", v -> { cfg.manaBarOverlayPositionY = v; cfg.markDirty(); })),
                    new GuiRow("acquiredutils.gui.setting.mana_bar_overlay_scale", "acquiredutils.gui.desc.mana_bar_overlay_scale", 22, 150, 40, 48,
                        (x, y, w, h) -> new ValueSliderWidget(x, y, w, h, cfg.manaBarOverlayScale, 1.0f, 0.5f, 2.0f, 0.05f, false, "x", v -> { cfg.manaBarOverlayScale = v; cfg.markDirty(); }))
                )
            )
        );
    }
}
