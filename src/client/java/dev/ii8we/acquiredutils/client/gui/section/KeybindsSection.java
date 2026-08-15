package dev.ii8we.acquiredutils.client.gui.section;

import dev.ii8we.acquiredutils.client.gui.AcquiredUtilsConfigScreen;
import dev.ii8we.acquiredutils.client.gui.widget.KeyListenerSlot;
import dev.ii8we.acquiredutils.client.gui.widget.KeybindWidget;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class KeybindsSection extends ModSection {
    private final KeyListenerSlot slotLockKeySlot = new KeyListenerSlot();

    public KeybindsSection(AcquiredUtilsConfigScreen screen) {
        super(screen);
    }

    @Override
    public String getId() {
        return "keybinds";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("acquiredutils.gui.tab.keybinds");
    }

    @Override
    public List<GuiRow> getRows() {
        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();
        return List.of(
            new GuiRow(
                "acquiredutils.gui.keybind.slot_lock",
                "acquiredutils.gui.desc.slot_lock_key",
                22, -1, 22, 40,
                (x, y, w, h) -> new KeybindWidget(
                    x, y, w, h,
                    slotLockKeySlot,
                    () -> cfg.slotLockKey,
                    keyCode -> {
                        cfg.slotLockKey = keyCode;
                        cfg.markDirty();
                        AcquiredUtilsConfig.saveIfDirty();
                    }
                )
            )
        );
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (slotLockKeySlot.current != null) {
            if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
                keyCode = -1;
            }
            slotLockKeySlot.current.applyKeyCode(keyCode);
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        slotLockKeySlot.clear();
    }
}
