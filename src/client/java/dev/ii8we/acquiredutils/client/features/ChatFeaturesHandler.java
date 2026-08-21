package dev.ii8we.acquiredutils.client.features;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only chat quality-of-life features.
 *
 * Chat combination replaces consecutive identical incoming player-chat
 * messages with one message and an (xN) suffix. Chat copying uses Ctrl+C
 * while the chat screen is open to copy the most recently received original
 * message, without the combination counter.
 */
public final class ChatFeaturesHandler {
    private static Component lastOriginalMessage;
    private static String lastMessageText;
    private static int repeatCount;
    private static MessageSignature lastSignature;
    private static boolean copyKeyWasDown;

    private ChatFeaturesHandler() {}

    public static void init() {
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, boundType, receptionTime) -> {
            AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();
            if (!cfg.chatCombinationEnabled) {
                lastOriginalMessage = message.copy();
                lastMessageText = message.getString();
                repeatCount = 1;
                lastSignature = signedMessage.signature();
                return true;
            }

            String text = message.getString();
            Minecraft client = Minecraft.getInstance();

            if (lastMessageText != null && lastMessageText.equals(text) && lastSignature != null) {
                repeatCount++;
                Component combined = lastOriginalMessage.copy()
                    .append(Component.literal(" (x" + repeatCount + ")"));
                client.gui.getChat().deleteMessage(lastSignature);
                client.gui.getChat().addMessage(combined, lastSignature, null);
            } else {
                lastOriginalMessage = message.copy();
                lastMessageText = text;
                repeatCount = 1;
                lastSignature = signedMessage.signature();
                client.gui.getChat().addMessage(message, lastSignature, null);
            }

            return false;
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> reset());
        ClientTickEvents.END_CLIENT_TICK.register(ChatFeaturesHandler::tickCopyShortcut);
    }

    private static void tickCopyShortcut(Minecraft client) {
        if (!AcquiredUtilsConfig.get().chatCopyingEnabled || !(client.screen instanceof ChatScreen)) {
            copyKeyWasDown = false;
            return;
        }

        Window window = client.getWindow();
        boolean ctrl = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
            || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean c = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_C);
        boolean down = ctrl && c;

        if (down && !copyKeyWasDown && lastOriginalMessage != null) {
            client.keyboardHandler.setClipboard(lastOriginalMessage.getString());
        }
        copyKeyWasDown = down;
    }

    public static void reset() {
        lastOriginalMessage = null;
        lastMessageText = null;
        repeatCount = 0;
        lastSignature = null;
        copyKeyWasDown = false;
    }
}
