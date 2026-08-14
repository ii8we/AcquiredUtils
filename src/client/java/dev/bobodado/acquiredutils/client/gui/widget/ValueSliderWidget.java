package dev.bobodado.acquiredutils.client.gui.widget;

import dev.bobodado.acquiredutils.client.gui.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class ValueSliderWidget extends AbstractSliderButton {

    private final double min;
    private final double max;
    private final double step;
    private final boolean percentage;
    private final String suffix;
    private final Consumer<Float> onChange;
    private final Runnable onReleaseAction;

    public ValueSliderWidget(int x, int y, int width, int height, float initialValue,
                             float min, float max, float step, boolean percentage,
                             String suffix, Consumer<Float> onChange, Runnable onReleaseAction) {
        super(x, y, width, height, Component.empty(), normalize(initialValue, min, max));
        this.min = min; this.max = max; this.step = step; this.percentage = percentage;
        this.suffix = suffix; this.onChange = onChange; this.onReleaseAction = onReleaseAction;
        snapToStep(); updateMessage();
    }

    public ValueSliderWidget(int x, int y, int width, int height, float initialValue,
                             float min, float max, float step, boolean percentage,
                             String suffix, Consumer<Float> onChange) {
        this(x, y, width, height, initialValue, min, max, step, percentage, suffix, onChange, () -> {});
    }

    private static double normalize(double value, double min, double max) {
        double clamped = Math.max(min, Math.min(max, value));
        return (clamped - min) / (max - min);
    }

    private double currentValue() { return min + this.value * (max - min); }

    private void snapToStep() {
        double raw = currentValue();
        double snapped = Math.round((raw - min) / step) * step + min;
        snapped = Math.max(min, Math.min(max, snapped));
        this.value = normalize(snapped, min, max);
    }

    private String formatValue() {
        double current = currentValue();
        if (percentage) return String.format("%.0f%%", current * 100.0);
        if (step >= 1.0) return String.format("%.0f%s", current, suffix);
        if (step >= 0.1) return String.format("%.1f%s", current, suffix);
        return String.format("%.2f%s", current, suffix);
    }

    @Override public void onRelease(MouseButtonEvent event) {
        snapToStep(); updateMessage(); applyValue(); onReleaseAction.run(); super.onRelease(event);
    }

    @Override protected void updateMessage() { setMessage(Component.literal(formatValue())); }

    @Override protected void applyValue() { snapToStep(); onChange.accept((float) currentValue()); updateMessage(); }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Theme theme = Theme.current();
        Minecraft minecraft = Minecraft.getInstance();

        int cy = getY() + height / 2 + sOffset(0);
        int left = getX() + 3;
        int right = getX() + width - 3;
        int trackH = Math.max(4, Math.min(6, height / 3));
        int trackY = cy - trackH / 2;

        // Dark rail with a purple fill. The value is intentionally not drawn
        // over the rail anymore, which keeps the slider readable at low resolutions.
        graphics.fill(
            left,
            trackY,
            right,
            trackY + trackH,
            theme.sliderTrack
        );

        int fillRight = left + (int) ((right - left) * this.value);
        if (fillRight > left) {
            graphics.fill(
                left,
                trackY,
                fillRight,
                trackY + trackH,
                theme.accent
            );
            graphics.fill(
                left,
                trackY,
                fillRight,
                trackY + 1,
                theme.accentBright
            );
        }

        // Small gold diamond thumb.
        int cx = Math.max(left, Math.min(right, fillRight));
        int r = Math.max(4, Math.min(6, height / 3));
        int gold = isSliderHighlighted() ? 0xFFFFE4A6 : 0xFFE5B85C;

        for (int dy = -r; dy <= r; dy++) {
            int span = r - Math.abs(dy);
            graphics.fill(
                cx - span,
                cy + dy,
                cx + span + 1,
                cy + dy + 1,
                gold
            );
        }
        graphics.fill(
            cx - 1,
            cy - r + 2,
            cx + 2,
            cy + r - 1,
            theme.accent
        );

        // Compact value badge sits above the thumb instead of in the track.
        Component message = getMessage();
        int valueW = Math.max(sOffset(34), minecraft.font.width(message) + 6);
        int valueH = Math.max(sOffset(12), 12);
        int valueX = Math.max(
            getX(),
            Math.min(getX() + width - valueW, cx - valueW / 2)
        );
        int valueY = Math.max(getY(), trackY - valueH - 2);

        graphics.fill(
            valueX,
            valueY,
            valueX + valueW,
            valueY + valueH,
            0xD9150D20
        );
        graphics.renderOutline(
            valueX,
            valueY,
            valueW,
            valueH,
            theme.frameMid
        );
        graphics.drawString(
            minecraft.font,
            message,
            valueX + (valueW - minecraft.font.width(message)) / 2,
            valueY + 2,
            theme.text,
            false
        );
    }

    private int sOffset(int base) {
        return Math.max(1, base);
    }

    private boolean isSliderHighlighted() {
        return isHovered() || isFocused();
    }

}
