package dev.bobodado.acquiredutils.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.bobodado.acquiredutils.AcquiredUtils;
import dev.bobodado.acquiredutils.client.gui.AcquiredUtilsConfigScreen;
import dev.bobodado.acquiredutils.client.gui.section.GeneralSection;
import dev.bobodado.acquiredutils.client.gui.section.OverlaysSection;
import dev.bobodado.acquiredutils.client.gui.section.ItemPickupSection;
import dev.bobodado.acquiredutils.client.gui.section.KeybindsSection;
import dev.bobodado.acquiredutils.client.pickup.ItemPickupNotifier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

public class AcquiredUtilsClient implements ClientModInitializer {

    private static final KeyMapping.Category CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath(AcquiredUtils.MOD_ID, "acquiredutils"));

    private static KeyMapping openConfigKey;

    @Override
    public void onInitializeClient() {
        AcquiredUtils.LOGGER.info("[AcquiredUtils] Initializing client entrypoint");

        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.acquiredutils.open_config",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_APOSTROPHE,
            CATEGORY
        ));

        SlotLockHandler.init();
        InventorySearchHandler.init();
        ContainerOverlayHandler.init();
        ItemPickupNotifier.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(createConfigScreen(null));
                }
            }
        });
    }

    public static AcquiredUtilsConfigScreen createConfigScreen(Screen parent) {
        AcquiredUtilsConfigScreen screen = new AcquiredUtilsConfigScreen(parent);
        screen.registerSection(new GeneralSection(screen));
        screen.registerSection(new ItemPickupSection(screen));
        screen.registerSection(new OverlaysSection(screen));
        screen.registerSection(new KeybindsSection(screen));
        return screen;
    }
}