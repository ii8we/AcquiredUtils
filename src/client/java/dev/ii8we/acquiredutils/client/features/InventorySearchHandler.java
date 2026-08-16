package dev.ii8we.acquiredutils.client.features;

import dev.ii8we.acquiredutils.client.features.ItemRarity;
import dev.ii8we.acquiredutils.client.features.ItemRarityDetector;
import dev.ii8we.acquiredutils.config.AcquiredUtilsConfig;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Search/highlight overlay for container screens.
 *
 * Text entry is delegated to Minecraft's native EditBox widget so character
 * input, keyboard layouts, selection, clipboard, deletion, and navigation all
 * follow the current Minecraft client implementation instead of manually
 * translating GLFW key codes.
 */
public final class InventorySearchHandler {

    private static final int SEARCH_WIDTH = 190;
    private static final int SEARCH_HEIGHT = 20;
    private static final int SEARCH_MARGIN = 4;
    private static final int SEARCH_MAX_LENGTH = 32;

    private static final Map<Screen, SearchState> STATES = new WeakHashMap<>();

    private static final class SearchState {
        EditBox editBox;
    }

    private InventorySearchHandler() {
    }

    public static boolean isFocused(Screen screen) {
        synchronized (STATES) {
            SearchState state = STATES.get(screen);
            return state != null && state.editBox != null && state.editBox.isFocused();
        }
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!AcquiredUtilsConfig.get().inventorySearchEnabled) {
                return;
            }
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
                return;
            }

            SearchBounds bounds = searchBounds(containerScreen);
            SearchState state = new SearchState();

            EditBox editBox = new EditBox(
                client.font,
                bounds.x(),
                bounds.y(),
                bounds.width(),
                bounds.height(),
                Component.literal("Search inventory")
            );
            editBox.setMaxLength(SEARCH_MAX_LENGTH);
            editBox.setHint(Component.literal("Search inventory..."));
            editBox.setTextColor(0xFFF2EAF7);
            editBox.setTextColorUneditable(0xFFAA9FB0);
            editBox.setBordered(true);
            editBox.setCanLoseFocus(true);

            state.editBox = editBox;
            synchronized (STATES) {
                STATES.put(screen, state);
            }

            // Screen.addRenderableWidget is protected in 1.21.11; the project
            // exposes it through the existing access-widener rather than a Mixin.
            screen.addRenderableWidget(editBox);

            ScreenEvents.afterRender(screen).register(
                (s, graphics, mouseX, mouseY, partialTick) ->
                    renderSearchTooltip(graphics, containerScreen, state, mouseX, mouseY)
            );

            // While the search box has focus, the inventory-toggle key (E by
            // default) belongs to text input instead of the container screen.
            // Block only that one screen-level action so typing the letter
            // "e" cannot close the inventory. The following character event
            // still reaches EditBox and inserts the actual character.
            net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.allowKeyPress(screen)
                .register((s, event) -> {
                    if (!state.editBox.isFocused()) {
                        return true;
                    }
                    return !client.options.keyInventory.matches(event);
                });
        });
    }

    public static java.util.List<Slot> renderHighlights(
        GuiGraphics graphics,
        AbstractContainerScreen<?> screen
    ) {
        java.util.List<Slot> highlighted = new java.util.ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return highlighted;
        }

        SearchState state;
        synchronized (STATES) {
            state = STATES.get(screen);
        }
        if (state == null || state.editBox == null) {
            return highlighted;
        }

        String normalized = state.editBox.getValue().toLowerCase(Locale.ROOT).trim();
        if (normalized.isEmpty()) {
            return highlighted;
        }

        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !stackMatches(stack, normalized)) {
                continue;
            }

            int sx = screen.leftPos + slot.x;
            int sy = screen.topPos + slot.y;
            graphics.fill(sx, sy, sx + 16, sy + 16, 0x3D9A6CFF);
            graphics.renderOutline(sx, sy, 16, 16, 0xFFBFA4FF);
            highlighted.add(slot);
        }

        return highlighted;
    }

    private static void renderSearchTooltip(
        GuiGraphics graphics,
        AbstractContainerScreen<?> screen,
        SearchState state,
        int mouseX,
        int mouseY
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || state.editBox == null) {
            return;
        }

        SearchBounds bounds = searchBounds(screen);
        if (mouseX >= bounds.x() && mouseX < bounds.x() + bounds.width()
            && mouseY >= bounds.y() && mouseY < bounds.y() + bounds.height()) {
            graphics.setTooltipForNextFrame(
                mc.font,
                java.util.List.of(
                    Component.literal("Inventory search"),
                    Component.literal("Click the bar to start typing"),
                    Component.literal("#<rarity>  e.g. #epic"),
                    Component.literal("!<enchantment>  e.g. !sharpness"),
                    Component.literal("Combine: #epic !sharpness sword")
                ),
                java.util.Optional.empty(),
                mouseX,
                mouseY
            );
        }
    }

    private static SearchBounds searchBounds(AbstractContainerScreen<?> screen) {
        int width = SEARCH_WIDTH;
        int height = SEARCH_HEIGHT;
        int x = screen.leftPos;

        if (x + width > screen.width - SEARCH_MARGIN) {
            x = Math.max(SEARCH_MARGIN, screen.width - width - SEARCH_MARGIN);
        }
        x = Math.max(SEARCH_MARGIN, x);

        int y = screen.topPos - height - SEARCH_MARGIN;
        if (y < SEARCH_MARGIN) {
            y = screen.topPos + SEARCH_MARGIN;
        }
        if (y + height > screen.height - SEARCH_MARGIN) {
            y = Math.max(SEARCH_MARGIN, screen.height - height - SEARCH_MARGIN);
        }

        return new SearchBounds(x, y, width, height);
    }

    private record SearchBounds(int x, int y, int width, int height) {
    }

    private static boolean stackMatches(ItemStack stack, String query) {
        if (query.isBlank()) return true;

        String[] tokens = query.split("\\s+");
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);

        for (String token : tokens) {
            if (token.isBlank()) continue;

            if (token.charAt(0) == '#') {
                String rarityName = token.substring(1);
                if (rarityName.isEmpty()) return false;

                ItemRarity rarity = ItemRarityDetector.detect(stack, Minecraft.getInstance().player);
                if (rarity == null || !rarity.name().toLowerCase(Locale.ROOT).equals(rarityName)) {
                    return false;
                }
                continue;
            }

            if (token.charAt(0) == '!') {
                String enchantmentName = token.substring(1).replace('-', '_');
                if (enchantmentName.isEmpty() || !hasEnchantment(stack, enchantmentName)) {
                    return false;
                }
                continue;
            }

            if (!name.contains(token) && !itemId.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasEnchantment(ItemStack stack, String query) {
        if (!EnchantmentHelper.hasAnyEnchantments(stack)) return false;

        String needle = query.toLowerCase(Locale.ROOT);
        ItemEnchantments enchantments = stack.getEnchantments();
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            String registered = holder.unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("")
                .toLowerCase(Locale.ROOT);
            String path = holder.unwrapKey()
                .map(key -> key.identifier().getPath())
                .orElse("")
                .toLowerCase(Locale.ROOT);
            String display = holder.value().description().getString().toLowerCase(Locale.ROOT);

            if (registered.contains(needle) || path.contains(needle) || display.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
