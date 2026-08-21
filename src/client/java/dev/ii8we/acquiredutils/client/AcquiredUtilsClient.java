package dev.ii8we.acquiredutils.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ii8we.acquiredutils.AcquiredUtils;
import dev.ii8we.acquiredutils.client.gui.AcquiredUtilsConfigScreen;
import dev.ii8we.acquiredutils.client.customkeybind.CustomKeybindManager;
import dev.ii8we.acquiredutils.client.gui.section.GeneralSection;
import dev.ii8we.acquiredutils.client.gui.section.KeybindsSection;
import dev.ii8we.acquiredutils.client.gui.section.QolSection;
import dev.ii8we.acquiredutils.client.gui.section.OverlaySection;
import dev.ii8we.acquiredutils.client.gui.section.PerformanceSection;
import dev.ii8we.acquiredutils.client.performance.PerformanceManager;
import dev.ii8we.acquiredutils.client.features.ItemPickupNotifier;
import dev.ii8we.acquiredutils.client.features.PlayerAbilitiesPanel;
import dev.ii8we.acquiredutils.client.features.HealthManaBarOverlay;
import dev.ii8we.acquiredutils.client.features.ChatFeaturesHandler;
import dev.ii8we.acquiredutils.client.playerclass.PlayerClassDataManager;
import dev.ii8we.acquiredutils.client.playerclass.PlayerHudDataReader;
import dev.ii8we.acquiredutils.client.playerclass.PlayerAbilityTreeReader;
import dev.ii8we.acquiredutils.client.features.PositionedItemInHandRenderer;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import dev.ii8we.acquiredutils.client.features.FeatureRegistry;
import dev.ii8we.acquiredutils.client.features.SlotLockHandler;
import dev.ii8we.acquiredutils.client.features.InventorySearchHandler;
import dev.ii8we.acquiredutils.client.features.InventoryFullWarningHandler;
import dev.ii8we.acquiredutils.client.features.ContainerOverlayHandler;
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
        PlayerClassDataManager.initialize();
        PlayerHudDataReader.init();
        PlayerAbilityTreeReader.init();
        CustomKeybindManager.initialize();
        FeatureRegistry.init();
        PerformanceManager.initialize();

        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.acquiredutils.open_config",
            InputConstants.Type.KEYSYM,
            AcquiredUtilsConfig.get().openConfigKey,
            CATEGORY
        ));






        syncConfiguredKeybinds();

        SlotLockHandler.init();
        InventorySearchHandler.init();
        InventoryFullWarningHandler.init();
        ContainerOverlayHandler.init();
        ItemPickupNotifier.init();
        PlayerAbilitiesPanel.init();
        HealthManaBarOverlay.init();
        ChatFeaturesHandler.init();


        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PositionedItemInHandRenderer.ensureInstalled();

            CustomKeybindManager.tick(client);

            while (openConfigKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(createConfigScreen(null));
                }
            }


        });
    }

    public static void setOpenConfigKey(int keyCode) {
        AcquiredUtilsConfig.get().openConfigKey = keyCode;
        if (openConfigKey != null) {
            openConfigKey.setKey(keyCode < 0
                ? InputConstants.UNKNOWN
                : InputConstants.Type.KEYSYM.getOrCreate(keyCode));
        }
    }


    public static void setSlotLockKey(int keyCode) {
        AcquiredUtilsConfig.get().slotLockKey = keyCode;
    }

    private static void syncConfiguredKeybinds() {
        setOpenConfigKey(AcquiredUtilsConfig.get().openConfigKey);
    }

    public static AcquiredUtilsConfigScreen createConfigScreen(Screen parent) {
        AcquiredUtilsConfigScreen screen = new AcquiredUtilsConfigScreen(parent);
        screen.registerSection(new GeneralSection(screen));
        screen.registerSection(new PerformanceSection(screen));
        screen.registerSection(new KeybindsSection(screen));
        screen.registerSection(new QolSection(screen));
        screen.registerSection(new OverlaySection(screen));
        return screen;
    }
}
