package dev.bobodado.acquiredutils.client.gui.section;

import dev.bobodado.acquiredutils.client.gui.AcquiredUtilsConfigScreen;
import dev.bobodado.acquiredutils.client.gui.PickupHudEditorScreen;
import dev.bobodado.acquiredutils.client.gui.widget.ThemedButtonWidget;
import dev.bobodado.acquiredutils.client.gui.widget.ValueSliderWidget;
import dev.bobodado.acquiredutils.client.gui.widget.ThemedCheckboxWidget;
import dev.bobodado.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ItemPickupSection extends ModSection {

    public ItemPickupSection(AcquiredUtilsConfigScreen screen) {
        super(screen);
    }

    @Override
    public String getId() {
        return "item_pickup";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("acquiredutils.gui.tab.item_pickup");
    }

    @Override
    public List<GuiRow> getRows() {
        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();

        return List.of(
            new GuiRow(
                "acquiredutils.gui.setting.item_pickup_notifier",
                "acquiredutils.gui.desc.item_pickup_notifier",
                22, 20, 20, 48,
                (x, y, w, h) -> new ThemedCheckboxWidget(
                    x + w - s(24), y + s(2), s(20), s(18),
                    () -> cfg.itemPickupNotifierEnabled,
                    checked -> { cfg.itemPickupNotifierEnabled = checked; cfg.markDirty(); }
                )
            ),
            new GuiRow(
                "acquiredutils.gui.setting.notification_duration",
                "acquiredutils.gui.desc.notification_duration",
                22, 150, 20, 48,
                (x, y, w, h) -> new ValueSliderWidget(
                    x, y, w, h,
                    cfg.notificationDuration,
                    0.5f,
                    10.0f,
                    0.5f,
                    false,
                    "s",
                    value -> {
                        cfg.notificationDuration = value;
                        cfg.markDirty();
                    }
                )
            ),
            new GuiRow(
                "acquiredutils.gui.setting.edit_hud",
                "acquiredutils.gui.desc.edit_hud",
                22, 150, 20, 48,
                (x, y, w, h) -> new ThemedButtonWidget(
                    x,
                    y,
                    w,
                    h,
                    Component.translatable("acquiredutils.gui.button.edit_hud"),
                    () -> screen.getMinecraft().setScreen(new PickupHudEditorScreen(screen))
                )
            )
        );
    }


}
