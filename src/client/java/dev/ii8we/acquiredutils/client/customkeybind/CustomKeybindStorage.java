package dev.ii8we.acquiredutils.client.customkeybind;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.ii8we.acquiredutils.AcquiredUtils;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class CustomKeybindStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "acquiredutils_custom_keybinds.json";

    private CustomKeybindStorage() {
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static List<CustomKeybind> load() {
        Path path = path();
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            StoredData data = GSON.fromJson(reader, StoredData.class);
            if (data == null || data.keybinds == null) {
                return new ArrayList<>();
            }
            List<CustomKeybind> result = new ArrayList<>();
            for (CustomKeybind keybind : data.keybinds) {
                if (keybind == null) {
                    continue;
                }
                if (keybind.getName().isBlank() || keybind.getActionText().isBlank()) {
                    continue;
                }
                result.add(keybind);
            }
            return result;
        } catch (Exception e) {
            AcquiredUtils.LOGGER.error("[AcquiredUtils] Failed to read custom keybinds", e);
            return new ArrayList<>();
        }
    }

    public static void save(List<CustomKeybind> keybinds) {
        Path path = path();
        Path temp = path.resolveSibling(FILE_NAME + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            StoredData data = new StoredData(new ArrayList<>(keybinds));
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception ignored) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            AcquiredUtils.LOGGER.error("[AcquiredUtils] Failed to write custom keybinds", e);
            try {
                Files.deleteIfExists(temp);
            } catch (Exception ignored) {
            }
        }
    }

    private record StoredData(List<CustomKeybind> keybinds) {
    }
}
