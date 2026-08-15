package dev.ii8we.acquiredutils.client.overlay;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class OverlayManager {
    @FunctionalInterface public interface Renderer {
        void render(GuiGraphics graphics, DeltaTracker tickCounter, int width, int height, OverlayPosition position);
    }
    private record Entry(Supplier<OverlayPosition> positionSupplier, Renderer renderer) {}
    private static final Map<Identifier, Entry> RENDERERS = new LinkedHashMap<>();
    private OverlayManager() {}
    public static void register(Identifier id, Supplier<OverlayPosition> positionSupplier, Renderer renderer){
        RENDERERS.putIfAbsent(id, new Entry(positionSupplier, renderer));
    }
    public static void renderAll(GuiGraphics graphics, DeltaTracker tickCounter, int width, int height){
        for(Entry entry: RENDERERS.values()){
            OverlayPosition position = entry.positionSupplier().get();
            entry.renderer().render(graphics,tickCounter,width,height,position);
        }
    }
}
