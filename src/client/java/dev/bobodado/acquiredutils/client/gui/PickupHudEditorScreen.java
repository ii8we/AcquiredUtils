package dev.bobodado.acquiredutils.client.gui;

import dev.bobodado.acquiredutils.config.AcquiredUtilsConfig;
import net.minecraft.client.gui.GuiGraphics;
import dev.bobodado.acquiredutils.client.gui.theme.Theme;
import dev.bobodado.acquiredutils.client.gui.widget.ThemedButtonWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class PickupHudEditorScreen extends Screen {

    private static final int PREVIEW_COLOR = 0xFFE0C7FF;

    private final Screen parent;
    private boolean dragging;

    public PickupHudEditorScreen(Screen parent) {
        super(Component.translatable("acquiredutils.gui.edit_hud.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(new ThemedButtonWidget(
            this.width / 2 - 50,
            this.height - 35,
            100,
            20,
            Component.translatable("gui.done"),
            this::onClose
        ));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Theme theme = Theme.current();

        // Keep the world visible behind the editor while adding a light purple tint.
        graphics.fill(0, 0, this.width, this.height, 0x700B0712);

        Component title = Component.translatable("acquiredutils.gui.edit_hud.title");
        int titleWidth = this.font.width(title);
        graphics.drawString(this.font, title, (this.width - titleWidth) / 2, 15, theme.text, true);

        Component hint = Component.translatable("acquiredutils.gui.edit_hud.hint");
        int hintWidth = this.font.width(hint);
        graphics.drawString(this.font, hint, (this.width - hintWidth) / 2, 30, theme.credit, false);

        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();
        int x = Math.round(cfg.notificationPositionX * this.width);
        int y = Math.round(cfg.notificationPositionY * this.height);
        String preview = "3x Shard";
        int previewWidth = this.font.width(preview);
        int anchorX = x;
        int drawX;

        if (cfg.notificationPositionX < 0.5f) {
            drawX = anchorX;
        } else if (cfg.notificationPositionX > 0.5f) {
            drawX = anchorX - previewWidth;
        } else {
            drawX = anchorX - previewWidth / 2;
        }

        graphics.fill(drawX - 5, y - 4, drawX + previewWidth + 5, y + 12, 0x66000000);
        graphics.fill(drawX - 2, y - 6, drawX, y + 14, theme.accent);
        graphics.drawString(this.font, Component.literal(preview), drawX, y, PREVIEW_COLOR, true);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Let normal widgets (especially the Done button) receive the click first.
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        if (event.button() == 0) {
            dragging = true;
            updatePosition(event.x(), event.y());
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (super.mouseReleased(event)) {
            dragging = false;
            return true;
        }

        if (event.button() == 0) {
            dragging = false;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (super.mouseDragged(event, dragX, dragY)) {
            return true;
        }

        if (dragging) {
            updatePosition(event.x(), event.y());
            return true;
        }

        return false;
    }

    private void updatePosition(double mouseX, double mouseY) {
        AcquiredUtilsConfig cfg = AcquiredUtilsConfig.get();
        cfg.notificationPositionX = (float) Math.max(0.0, Math.min(1.0, mouseX / this.width));
        cfg.notificationPositionY = (float) Math.max(0.0, Math.min(1.0, mouseY / this.height));
        cfg.markDirty();
    }

    @Override
    public void onClose() {
        AcquiredUtilsConfig.saveIfDirty();
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
