package dev.ii8we.acquiredutils.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ii8we.acquiredutils.AcquiredUtils;
import dev.ii8we.acquiredutils.client.gui.AcquiredUtilsConfigScreen;
import dev.ii8we.acquiredutils.client.gui.section.GeneralSection;
import dev.ii8we.acquiredutils.client.gui.section.KeybindsSection;
import dev.ii8we.acquiredutils.client.gui.section.QolSection;
import dev.ii8we.acquiredutils.client.gui.section.OverlaySection;
import dev.ii8we.acquiredutils.client.pickup.ItemPickupNotifier;
import dev.ii8we.acquiredutils.client.render.PositionedItemInHandRenderer;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import dev.ii8we.acquiredutils.client.feature.FeatureRegistry;
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
        AcquiredUtilsConfig.load();
        FeatureRegistry.init();

        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.acquiredutils.open_config",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_APOSTROPHE,
            CATEGORY
        ));

        SlotLockHandler.init();
        InventorySearchHandler.init();
        InventoryFullWarningHandler.init();
        ContainerOverlayHandler.init();
        ItemPickupNotifier.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PositionedItemInHandRenderer.ensureInstalled();
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
        screen.registerSection(new KeybindsSection(screen));
        screen.registerSection(new QolSection(screen));
        screen.registerSection(new OverlaySection(screen));
        return screen;
    }
}