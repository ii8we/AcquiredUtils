package dev.ii8we.acquiredutils.client.gui.section;

import dev.ii8we.acquiredutils.client.gui.AcquiredUtilsConfigScreen;
import dev.ii8we.acquiredutils.client.gui.widget.FeatureControlWidget;
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
            )
        );
    }
}
