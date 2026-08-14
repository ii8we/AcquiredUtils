package dev.bobodado.acquiredutils.client.gui.section;

import dev.bobodado.acquiredutils.client.gui.AcquiredUtilsConfigScreen;
import dev.bobodado.acquiredutils.client.gui.widget.ValueSliderWidget;
import dev.bobodado.acquiredutils.config.AcquiredUtilsConfig;
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

        return List.of(
            new GuiRow(
                "acquiredutils.gui.setting.menu_scale",
                "acquiredutils.gui.desc.menu_scale",
                22, 150, 20, 48,
                (x, y, w, h) -> new ValueSliderWidget(
                    x, y, w, h,
                    cfg.menuScale,
                    0.5f,
                    1.5f,
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
}
