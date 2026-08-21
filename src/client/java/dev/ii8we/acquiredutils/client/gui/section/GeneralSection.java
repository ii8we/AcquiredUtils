package dev.ii8we.acquiredutils.client.gui.section;

import dev.ii8we.acquiredutils.client.gui.AcquiredUtilsConfigScreen;
import dev.ii8we.acquiredutils.client.gui.widget.ThemedCheckboxWidget;
import dev.ii8we.acquiredutils.client.gui.widget.FeatureControlWidget;
import dev.ii8we.acquiredutils.client.gui.widget.ValueSliderWidget;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.network.chat.Component;

import java.util.List;

public class GeneralSection extends ModSection {

    public GeneralSection(AcquiredUtilsConfigScreen screen) {
        super(screen);
    }

    @Override
    public String getId() {
        return "general";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("acquiredutils.gui.tab.general");
    }

    @Override
    public List<GuiRow> getRows() {
        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();

        // These sliders control only menu presentation. Feature-specific scales
        // remain under their respective feature settings.
        return List.of(
            new GuiRow(
                "acquiredutils.gui.setting.menu_scale",
                "acquiredutils.gui.desc.menu_scale",
                22, 150, 40, 48,
                (x, y, w, h) -> new ValueSliderWidget(
                    x, y, w, h,
                    cfg.menuScale,
                    1.0f,
                    0.5f,
                    2.5f,
                    0.05f,
                    true,
                    "",
                    value -> {
                        cfg.menuScale = value;
                        cfg.markDirty();
                    },
                    screen::scheduleRebuild
                )
            )
        );
    }

    @Override
    public List<GuiFeature> getFeatures() {
        return List.of();
    }
}
