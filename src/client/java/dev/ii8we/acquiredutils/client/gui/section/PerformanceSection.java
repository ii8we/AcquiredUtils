package dev.ii8we.acquiredutils.client.gui.section;

import dev.ii8we.acquiredutils.client.gui.AcquiredUtilsConfigScreen;
import dev.ii8we.acquiredutils.client.gui.widget.FeatureControlWidget;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class PerformanceSection extends ModSection {

    public PerformanceSection(AcquiredUtilsConfigScreen screen) {
        super(screen);
    }

    @Override
    public String getId() {
        return "performance";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("acquiredutils.gui.tab.performance");
    }

    @Override
    public List<GuiFeature> getFeatures() {
        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();

        return List.of(
            new GuiFeature(
                "disable_glowing_effects",
                "acquiredutils.gui.setting.disable_glowing_effects",
                "acquiredutils.gui.desc.disable_glowing_effects",
                () -> cfg.disableGlowingEffects,
                enabled -> {
                    cfg.disableGlowingEffects = enabled;
                    cfg.markDirty();
                    AcquiredUtilsConfig.saveIfDirty();
                    screen.scheduleRebuild();
                },
                (x, y, w, h) -> new FeatureControlWidget(
                    x, y, w, h,
                    "disable_glowing_effects",
                    screen::scheduleRebuild
                ),
                List.of()
            )
        );
    }
}
