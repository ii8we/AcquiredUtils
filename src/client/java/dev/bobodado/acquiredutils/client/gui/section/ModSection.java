package dev.bobodado.acquiredutils.client.gui.section;

import dev.bobodado.acquiredutils.client.gui.AcquiredUtilsConfigScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;

public abstract class ModSection {

    protected final AcquiredUtilsConfigScreen screen;

    public ModSection(AcquiredUtilsConfigScreen screen) {
        this.screen = screen;
    }

    public abstract String getId();

    public abstract Component getDisplayName();

    public List<GuiRow> getRows() {
        return Collections.emptyList();
    }

    protected int s(int base) {
        return screen.s(base);
    }

    protected float scale() {
        return screen.getMenuScale();
    }

    protected <T extends AbstractWidget> T addWidget(T widget) {
        screen.addSectionWidget(widget);
        return widget;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
                       int contentX, int contentY, int contentWidth, int contentHeight) {
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public void onClose() {
    }
}