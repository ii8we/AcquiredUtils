package dev.ii8we.acquiredutils.client.customkeybind;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Manages player-created single-action keybinds. A leading '/' means command;
 * all other text is sent as chat. Bindings support Control, Alt, and Shift
 * modifiers and are edge-triggered directly from the Minecraft window.
 */
public final class CustomKeybindManager {
    private static final List<CustomKeybind> KEYBINDS = new ArrayList<>();
    private static final Map<String, Boolean> PRESSED_STATES = new HashMap<>();
    private static boolean initialized;

    private CustomKeybindManager() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        List<CustomKeybind> loaded = CustomKeybindStorage.load();
        List<CustomKeybind> valid = new ArrayList<>();
        for (CustomKeybind keybind : loaded) {
            if (isValidForRegistration(keybind, valid)) {
                valid.add(keybind);
                PRESSED_STATES.put(keybind.getId(), false);
            }
        }

        KEYBINDS.clear();
        KEYBINDS.addAll(valid);
        if (valid.size() != loaded.size()) {
            save();
        }
    }

    public static List<CustomKeybind> getAll() {
        return Collections.unmodifiableList(KEYBINDS);
    }

    public static CustomKeybind create(String name, int keyCode, boolean control, boolean alt, boolean shift, String text) {
        CustomKeybind keybind = new CustomKeybind(name, keyCode, control, alt, shift, text);
        add(keybind);
        return keybind;
    }

    public static void add(CustomKeybind keybind) {
        requireInitialized();
        validate(keybind);
        KEYBINDS.add(keybind);
        PRESSED_STATES.put(keybind.getId(), false);
        save();
    }

    public static boolean update(CustomKeybind updated) {
        requireInitialized();
        if (!isValidDefinition(updated) || isBindingConflicting(updated.getKeyCode(), updated.isControl(), updated.isAlt(), updated.isShift(), updated.getId())) {
            return false;
        }

        for (int i = 0; i < KEYBINDS.size(); i++) {
            CustomKeybind current = KEYBINDS.get(i);
            if (!current.getId().equals(updated.getId())) {
                continue;
            }
            KEYBINDS.set(i, updated.copy());
            PRESSED_STATES.put(updated.getId(), false);
            save();
            return true;
        }
        return false;
    }

    public static boolean remove(String id) {
        requireInitialized();
        if (id == null) {
            return false;
        }

        CustomKeybind removed = null;
        for (CustomKeybind keybind : KEYBINDS) {
            if (id.equals(keybind.getId())) {
                removed = keybind;
                break;
            }
        }
        if (removed == null) {
            return false;
        }

        KEYBINDS.remove(removed);
        PRESSED_STATES.remove(id);
        save();
        return true;
    }

    public static boolean isKeyConflicting(int keyCode, String ignoredId) {
        return isBindingConflicting(keyCode, false, false, false, ignoredId);
    }

    public static boolean isBindingConflicting(int keyCode, boolean control, boolean alt, boolean shift, String ignoredId) {
        if (keyCode < 0) {
            return false;
        }

        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();
        if (!control && !alt && !shift && isBuiltInKey(keyCode, cfg)) {
            return true;
        }

        for (CustomKeybind keybind : KEYBINDS) {
            if (!keybind.getId().equals(ignoredId)
                && keybind.getKeyCode() == keyCode
                && keybind.isControl() == control
                && keybind.isAlt() == alt
                && keybind.isShift() == shift) {
                return true;
            }
        }
        return false;
    }

    public static boolean isReservedKey(int keyCode, String ignoredId) {
        return isKeyConflicting(keyCode, ignoredId);
    }

    public static boolean isReservedBinding(int keyCode, boolean control, boolean alt, boolean shift, String ignoredId) {
        return isBindingConflicting(keyCode, control, alt, shift, ignoredId);
    }

    public static void tick(Minecraft client) {
        if (!initialized || client.player == null || client.getWindow() == null) {
            resetPressedStates();
            return;
        }

        boolean gameplay = client.screen == null && client.getConnection() != null;
        for (CustomKeybind keybind : KEYBINDS) {
            boolean down = isBindingDown(client, keybind);
            boolean wasDown = PRESSED_STATES.getOrDefault(keybind.getId(), false);
            PRESSED_STATES.put(keybind.getId(), down);

            if (gameplay && down && !wasDown) {
                execute(client, keybind);
            }
        }
    }

    private static boolean isBindingDown(Minecraft client, CustomKeybind keybind) {
        int keyCode = keybind.getKeyCode();
        if (keyCode < 0) {
            return false;
        }

        boolean baseDown = InputConstants.isKeyDown(client.getWindow(), keyCode);
        if (!baseDown) {
            return false;
        }

        boolean controlDown = isModifierDown(client, GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean altDown = isModifierDown(client, GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT);
        boolean shiftDown = isModifierDown(client, GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT);

        return controlDown == keybind.isControl()
            && altDown == keybind.isAlt()
            && shiftDown == keybind.isShift();
    }

    private static boolean isModifierDown(Minecraft client, int leftKey, int rightKey) {
        return InputConstants.isKeyDown(client.getWindow(), leftKey)
            || InputConstants.isKeyDown(client.getWindow(), rightKey);
    }

    private static void execute(Minecraft client, CustomKeybind keybind) {
        String text = keybind.getActionText().trim();
        if (text.isBlank() || client.getConnection() == null) {
            return;
        }

        if (text.startsWith("/")) {
            String command = text.substring(1).trim();
            if (!command.isBlank()) {
                client.getConnection().sendCommand(command);
            }
            return;
        }

        client.getConnection().sendChat(text);
    }

    private static boolean isValidForRegistration(CustomKeybind keybind, List<CustomKeybind> previous) {
        if (!isValidDefinition(keybind)) {
            return false;
        }
        int keyCode = keybind.getKeyCode();
        if (keyCode < 0) {
            return true;
        }

        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();
        if (!keybind.isControl() && !keybind.isAlt() && !keybind.isShift() && isBuiltInKey(keyCode, cfg)) {
            return false;
        }

        for (CustomKeybind existing : previous) {
            if (sameBinding(existing, keybind)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBuiltInKey(int keyCode, AcquiredUtilsConfig cfg) {
        return keyCode == cfg.slotLockKey || keyCode == cfg.openConfigKey;
    }

    private static boolean sameBinding(CustomKeybind first, CustomKeybind second) {
        return first.getKeyCode() == second.getKeyCode()
            && first.isControl() == second.isControl()
            && first.isAlt() == second.isAlt()
            && first.isShift() == second.isShift();
    }

    private static boolean isValidDefinition(CustomKeybind keybind) {
        return keybind != null
            && !keybind.getName().isBlank()
            && !keybind.getActionText().isBlank();
    }

    private static void validate(CustomKeybind keybind) {
        if (!isValidDefinition(keybind)) {
            throw new IllegalArgumentException("Name and action text cannot be blank.");
        }
        if (isBindingConflicting(keybind.getKeyCode(), keybind.isControl(), keybind.isAlt(), keybind.isShift(), keybind.getId())) {
            throw new IllegalArgumentException("That key combination is already in use.");
        }
    }

    private static void resetPressedStates() {
        if (!PRESSED_STATES.isEmpty()) {
            for (String id : new HashSet<>(PRESSED_STATES.keySet())) {
                PRESSED_STATES.put(id, false);
            }
        }
    }

    public static void resetForTests() {
        resetPressedStates();
    }

    private static void save() {
        CustomKeybindStorage.save(KEYBINDS);
    }

    private static void requireInitialized() {
        if (!initialized) {
            throw new IllegalStateException("CustomKeybindManager is not initialized.");
        }
    }
}
