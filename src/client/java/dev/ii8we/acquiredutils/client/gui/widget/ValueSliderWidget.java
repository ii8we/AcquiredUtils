package dev.ii8we.acquiredutils.client.gui.widget;

import dev.ii8we.acquiredutils.client.gui.theme.Theme;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;

public class ValueSliderWidget extends AbstractSliderButton {
    private static final int SLIDER_AREA_HEIGHT = 22;
    private static final int RESET_SIZE = 16;

    private final double min;
    private final double max;
    private final double step;
    private final boolean percentage;
    private final String suffix;
    private final Consumer<Float> onChange;
    private final Runnable onReleaseAction;
    private final float defaultValue;
    private boolean draggingWithMouse;
    private boolean editingValue;
    private String editBuffer = "";
    private int editCursor;
    private boolean editSelectionActive;

    public ValueSliderWidget(int x, int y, int width, int height, float initialValue,
                             float defaultValue, float min, float max, float step, boolean percentage,
                             String suffix, Consumer<Float> onChange, Runnable onReleaseAction) {
        super(x, y, width, height, Component.empty(), normalize(initialValue, min, max));
        this.min = min;
        this.max = max;
        if (!Float.isFinite(min) || !Float.isFinite(max) || max <= min) {
            throw new IllegalArgumentException("Slider max must be greater than min");
        }
        if (!Float.isFinite(step) || step <= 0.0f) {
            throw new IllegalArgumentException("Slider step must be greater than zero");
        }
        this.step = step;
        this.percentage = percentage;
        this.suffix = suffix;
        this.onChange = onChange;
        this.onReleaseAction = onReleaseAction;
        this.defaultValue = (float) clamp(defaultValue);
        snapToStep();
        updateMessage();
    }

    public ValueSliderWidget(int x, int y, int width, int height, float initialValue,
                             float defaultValue, float min, float max, float step, boolean percentage,
                             String suffix, Consumer<Float> onChange) {
        this(x, y, width, height, initialValue, defaultValue, min, max, step, percentage, suffix, onChange, () -> {});
    }

    private static double normalize(double value, double min, double max) {
        if (max <= min) {
            return 0.0;
        }
        double clamped = Math.max(min, Math.min(max, value));
        return (clamped - min) / (max - min);
    }

    private double clamp(double value) {
        return Math.max(min, Math.min(max, value));
    }

    private double currentValue() {
        return min + this.value * (max - min);
    }

    private void snapToStep() {
        double raw = currentValue();
        double snapped = Math.round((raw - min) / step) * step + min;
        snapped = clamp(snapped);
        this.value = normalize(snapped, min, max);
    }

    private String formatValue() {
        return formatValue(currentValue());
    }

    private String formatValue(double current) {
        if (percentage) return String.format(Locale.ROOT, "%.0f%%", current * 100.0);
        if (step >= 1.0) return String.format(Locale.ROOT, "%.0f%s", current, suffix);
        if (step >= 0.1) return String.format(Locale.ROOT, "%.1f%s", current, suffix);
        return String.format(Locale.ROOT, "%.2f%s", current, suffix);
    }

    private int valueBadgeWidth(Minecraft minecraft) {
        int minWidth = minecraft.font.width(Component.literal(formatValue(min)));
        int maxWidth = minecraft.font.width(Component.literal(formatValue(max)));
        int currentWidth = minecraft.font.width(getMessage());
        return Math.max(36, Math.max(minWidth, Math.max(maxWidth, currentWidth)) + 10);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return false;
        }

        if (isResetHovered(event.x(), event.y())) {
            if (editingValue) {
                cancelEditing();
            }
            resetToDefault();
            setFocused(true);
            return true;
        }

        if (isValueBadgeHovered(event.x(), event.y())) {
            beginEditing();
            return true;
        }

        if (editingValue) {
            if (!commitEdit()) {
                cancelEditing();
            }
        }

        if (isSliderAreaHovered(event.x(), event.y())) {
            stopEditing();
            draggingWithMouse = true;
            setFocused(true);
            setValueFromMouse(event.x());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingWithMouse && event.button() == 0) {
            setValueFromMouse(event.x());
            return true;
        }
        return false;
    }

    public boolean isDraggingWithMouse() {
        return draggingWithMouse;
    }

    /** Returns whether the widget is visible. */
    public boolean isVisible() {
        return this.visible;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() != 0 || !draggingWithMouse) {
            return false;
        }

        draggingWithMouse = false;
        snapToStep();
        updateMessage();
        onReleaseAction.run();
        return true;
    }

    private int sliderAreaBottom() {
        return getY() + Math.min(getHeight(), SLIDER_AREA_HEIGHT);
    }

    private int resetX() {
        return getX() + Math.max(0, getWidth() - RESET_SIZE);
    }

    private int resetY() {
        int centerY = getY() + Math.min(SLIDER_AREA_HEIGHT, getHeight()) / 2;
        return centerY - RESET_SIZE / 2;
    }

    private boolean isSliderAreaHovered(double mouseX, double mouseY) {
        if (isResetHovered(mouseX, mouseY)) {
            return false;
        }
        return mouseX >= getX()
            && mouseX < getX() + getWidth()
            && mouseY >= getY()
            && mouseY < sliderAreaBottom();
    }

    private boolean isResetHovered(double mouseX, double mouseY) {
        int x = resetX();
        int y = resetY();
        return mouseX >= x
            && mouseX < x + RESET_SIZE
            && mouseY >= y
            && mouseY < y + RESET_SIZE;
    }

    private boolean isValueBadgeHovered(double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        int valueW = valueBadgeWidth(minecraft);
        int contentRight = Math.max(getX() + 1, resetX() - 5);
        int valueX = Math.max(getX() + 1, contentRight - valueW);
        int centerY = getY() + Math.min(SLIDER_AREA_HEIGHT, getHeight()) / 2;
        int valueH = Math.min(18, Math.max(1, SLIDER_AREA_HEIGHT - 2));
        int valueY = centerY - valueH / 2;
        return mouseX >= valueX
            && mouseX < valueX + valueW
            && mouseY >= valueY
            && mouseY < valueY + valueH;
    }

    private void beginEditing() {
        draggingWithMouse = false;
        setFocused(true);
        editingValue = true;
        editBuffer = inputValue(currentValue());
        editCursor = editBuffer.length();
        editSelectionActive = true;
        updateMessage();
    }

    private void cancelEditing() {
        editingValue = false;
        editBuffer = "";
        editCursor = 0;
        editSelectionActive = false;
        updateMessage();
    }

    private void stopEditing() {
        if (!editingValue) {
            return;
        }
        cancelEditing();
    }

    private boolean hasSelection() {
        return editSelectionActive && !editBuffer.isEmpty();
    }

    private void selectAll() {
        editCursor = editBuffer.length();
        editSelectionActive = true;
    }

    private void clearSelection() {
        editSelectionActive = false;
    }

    private void replaceSelection(String text) {
        if (!hasSelection()) {
            editBuffer = editBuffer.substring(0, editCursor) + text + editBuffer.substring(editCursor);
            editCursor += text.length();
            return;
        }

        editBuffer = text;
        editCursor = text.length();
        editSelectionActive = false;
    }

    private String inputValue(double value) {
        if (percentage) {
            return formatEditableNumber(value * 100.0);
        }
        return formatEditableNumber(value);
    }

    private String formatEditableNumber(double value) {
        if (step >= 1.0) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        if (step >= 0.1) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private boolean commitEdit() {
        if (!editingValue) {
            return false;
        }
        String text = editBuffer.trim();
        if (text.isEmpty() || text.equals("-") || text.equals(".") || text.equals("-.")) {
            return false;
        }

        try {
            double parsed = Double.parseDouble(text.replace(',', '.'));
            if (percentage) {
                parsed /= 100.0;
            }

            double clamped = clamp(parsed);
            this.value = normalize(clamped, min, max);
            onChange.accept((float) clamped);
            updateMessage();
            onReleaseAction.run();
        } catch (NumberFormatException ignored) {
            // Keep the editor active so the user can correct the value.
            return false;
        }

        stopEditing();
        return true;
    }

    private void resetToDefault() {
        setAbsoluteValue(defaultValue);
        updateMessage();
    }

    private void setValueFromMouse(double mouseX) {
        Minecraft minecraft = Minecraft.getInstance();
        int valueW = valueBadgeWidth(minecraft);
        int contentRight = Math.max(getX() + 1, resetX() - 5);
        int valueX = Math.max(getX() + 1, contentRight - valueW);

        int trackLeft = getX() + 3;
        int trackRight = valueX - 8;
        if (trackRight <= trackLeft) {
            trackRight = Math.max(trackLeft + 1, getX() + getWidth() - 3);
        }

        int innerLeft = trackLeft + 1;
        int innerRight = Math.max(innerLeft + 1, trackRight - 1);
        double normalized = (mouseX - innerLeft) / (double) (innerRight - innerLeft);
        normalized = Math.max(0.0, Math.min(1.0, normalized));

        this.value = normalized;
        snapToStep();
        applyValue();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!isFocused()) {
            return false;
        }

        int key = event.key();

        if (editingValue) {
            boolean controlDown = (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
            if (controlDown && key == GLFW.GLFW_KEY_A) {
                selectAll();
                updateMessage();
                return true;
            }

            switch (key) {
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    return commitEdit();
                }
                case GLFW.GLFW_KEY_ESCAPE -> {
                    cancelEditing();
                    return true;
                }
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (hasSelection()) {
                        editBuffer = "";
                        editCursor = 0;
                        editSelectionActive = false;
                    } else if (editCursor > 0) {
                        editBuffer = editBuffer.substring(0, editCursor - 1) + editBuffer.substring(editCursor);
                        editCursor--;
                    }
                    updateMessage();
                    return true;
                }
                case GLFW.GLFW_KEY_DELETE -> {
                    if (hasSelection()) {
                        editBuffer = "";
                        editCursor = 0;
                        editSelectionActive = false;
                    } else if (editCursor < editBuffer.length()) {
                        editBuffer = editBuffer.substring(0, editCursor) + editBuffer.substring(editCursor + 1);
                    }
                    updateMessage();
                    return true;
                }
                case GLFW.GLFW_KEY_LEFT -> {
                    if (hasSelection()) {
                        editCursor = 0;
                        clearSelection();
                    } else {
                        editCursor = Math.max(0, editCursor - 1);
                    }
                    updateMessage();
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    if (hasSelection()) {
                        editCursor = editBuffer.length();
                        clearSelection();
                    } else {
                        editCursor = Math.min(editBuffer.length(), editCursor + 1);
                    }
                    updateMessage();
                    return true;
                }
                case GLFW.GLFW_KEY_HOME -> {
                    editCursor = 0;
                    clearSelection();
                    updateMessage();
                    return true;
                }
                case GLFW.GLFW_KEY_END -> {
                    editCursor = editBuffer.length();
                    clearSelection();
                    updateMessage();
                    return true;
                }
                default -> {
                    return true;
                }
            }
        }

        if (key == com.mojang.blaze3d.platform.InputConstants.KEY_LEFT || key == com.mojang.blaze3d.platform.InputConstants.KEY_DOWN) {
            setAbsoluteValue(currentValue() - step);
            return true;
        }
        if (key == com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT || key == com.mojang.blaze3d.platform.InputConstants.KEY_UP) {
            setAbsoluteValue(currentValue() + step);
            return true;
        }
        if (key == com.mojang.blaze3d.platform.InputConstants.KEY_HOME) {
            setAbsoluteValue(min);
            return true;
        }
        if (key == com.mojang.blaze3d.platform.InputConstants.KEY_END) {
            setAbsoluteValue(max);
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!isFocused() || !editingValue) {
            return false;
        }

        int codePoint = event.codepoint();
        if ((codePoint >= '0' && codePoint <= '9') || codePoint == '.' || codePoint == '-' || codePoint == ',') {
            if (codePoint == '-') {
                if (hasSelection()) {
                    editBuffer = "";
                    editCursor = 0;
                    editSelectionActive = false;
                }
                if (editCursor != 0 || editBuffer.indexOf('-') >= 0) {
                    return true;
                }
            }

            if (codePoint == '.' || codePoint == ',') {
                boolean hasSeparator = editBuffer.indexOf('.') >= 0 || editBuffer.indexOf(',') >= 0;
                if (hasSelection()) {
                    hasSeparator = false;
                }
                if (hasSeparator) {
                    return true;
                }
            }

            String typed = Character.toString(codePoint == ',' ? '.' : codePoint);
            replaceSelection(typed);
            updateMessage();
            return true;
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Shift + wheel adjusts the slider.
        if (!isShiftDown()) return false;
        if (!isSliderAreaHovered(mouseX, mouseY)) return false;
        if (scrollY == 0.0) return false;

        double multiplier = scrollY > 0 ? 1.0 : -1.0;
        setAbsoluteValue(currentValue() + step * multiplier);
        return true;
    }

    private boolean isShiftDown() {
        Minecraft minecraft = Minecraft.getInstance();
        long window = minecraft.getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private void setAbsoluteValue(double absolute) {
        double clamped = clamp(absolute);
        this.value = normalize(clamped, min, max);
        snapToStep();
        applyValue();
        onReleaseAction.run();
    }

    public void clearInteractiveFocus() {
        if (editingValue) {
            if (!commitEdit()) {
                cancelEditing();
            }
        }
        draggingWithMouse = false;
        setFocused(false);
    }

    @Override
    protected void updateMessage() {
        if (editingValue) {
            if (editSelectionActive) {
                setMessage(Component.literal(editBuffer));
            } else {
                String before = editBuffer.substring(0, Math.min(editCursor, editBuffer.length()));
                String after = editBuffer.substring(Math.min(editCursor, editBuffer.length()));
                setMessage(Component.literal(before + "|" + after));
            }
            return;
        }
        setMessage(Component.literal(formatValue()));
    }

    @Override
    protected void applyValue() {
        snapToStep();
        onChange.accept((float) currentValue());
        updateMessage();
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Theme theme = Theme.current();
        Minecraft minecraft = Minecraft.getInstance();

        final boolean active = isSliderAreaHovered(mouseX, mouseY) || isFocused();
        final int centerY = getY() + Math.min(SLIDER_AREA_HEIGHT, getHeight()) / 2;
        final int outerLeft = getX();
        final int outerRight = getX() + getWidth();
        final int outerTop = getY() + 1;
        final int outerBottom = getY() + Math.min(SLIDER_AREA_HEIGHT, getHeight()) - 1;

        int valueW = valueBadgeWidth(minecraft);
        int valueH = Math.min(18, Math.max(1, SLIDER_AREA_HEIGHT - 2));
        int resetX = resetX();
        int contentRight = Math.max(outerLeft + 1, resetX - 5);
        int valueX = Math.max(outerLeft + 1, contentRight - valueW);
        int valueY = centerY - valueH / 2;

        // The reset control stays beside the slider on the same horizontal row.
        int trackLeft = outerLeft + 3;
        int trackRight = valueX - 8;
        if (trackRight <= trackLeft) {
            trackRight = Math.max(trackLeft + 1, outerRight - 3);
        }

        int trackHeight = 7;
        int trackY = centerY - trackHeight / 2;

        graphics.fill(trackLeft, trackY + 1, trackRight, trackY + trackHeight + 1, theme.shadow);
        graphics.fill(trackLeft, trackY, trackRight, trackY + trackHeight, theme.frameOuter);

        int innerLeft = trackLeft + 1;
        int innerRight = Math.max(innerLeft + 1, trackRight - 1);
        int innerTop = trackY + 1;
        int innerBottom = trackY + trackHeight - 1;
        graphics.fill(innerLeft, innerTop, innerRight, innerBottom, theme.sliderTrack);

        int fillRight = innerLeft + (int) ((innerRight - innerLeft) * this.value);
        fillRight = Math.max(innerLeft, Math.min(innerRight, fillRight));
        if (fillRight > innerLeft) {
            graphics.fill(innerLeft, innerTop, fillRight, innerBottom, active ? theme.accentBright : theme.accent);
            graphics.fill(innerLeft, innerTop, fillRight, innerTop + 1, active ? theme.text : theme.accentBright);
        }
        graphics.fill(innerLeft, innerTop, Math.min(innerLeft + 1, innerRight), innerBottom, theme.frameMid);

        int thumbX = Math.max(innerLeft, Math.min(innerRight, fillRight));
        int thumbW = 10;
        int thumbH = Math.min(18, Math.max(12, SLIDER_AREA_HEIGHT - 2));
        int thumbLeft = thumbX - thumbW / 2;
        int thumbTop = centerY - thumbH / 2;
        graphics.fill(thumbLeft + 1, thumbTop + 1, thumbLeft + thumbW + 1, thumbTop + thumbH + 1, theme.shadow);
        graphics.fill(thumbLeft, thumbTop, thumbLeft + thumbW, thumbTop + thumbH, active ? theme.frameAccent : theme.frameMid);
        graphics.fill(thumbLeft + 1, thumbTop + 1, thumbLeft + thumbW - 1, thumbTop + thumbH - 1, theme.accentBright);
        graphics.fill(thumbLeft + 2, thumbTop + 2, thumbLeft + thumbW - 2, thumbTop + thumbH - 2, active ? theme.accent : theme.frameMid);
        graphics.fill(thumbLeft + 3, thumbTop + 2, thumbLeft + thumbW - 3, thumbTop + 3, theme.text);

        int valueOuter = active ? theme.frameAccent : theme.frameMid;
        graphics.fill(valueX, valueY, valueX + valueW, valueY + valueH, 0xE0160D20);
        graphics.renderOutline(valueX, valueY, valueW, valueH, valueOuter);

        int textY = valueY + Math.max(1, (valueH - minecraft.font.lineHeight) / 2);
        graphics.drawString(
            minecraft.font,
            message,
            valueX + (valueW - minecraft.font.width(message)) / 2,
            textY,
            active ? theme.accentBright : theme.text,
            false
        );

        if (isFocused()) {
            graphics.renderOutline(outerLeft, outerTop, getWidth(), Math.max(1, outerBottom - outerTop + 1), theme.accent);
        }

        int resetY = resetY();
        boolean resetHovered = isResetHovered(mouseX, mouseY);
        if (resetHovered) {
            graphics.fill(resetX - 2, resetY - 2, resetX + RESET_SIZE + 2, resetY + RESET_SIZE + 2, 0x55302030);
            graphics.setTooltipForNextFrame(minecraft.font, Component.literal("Reset to default"), mouseX, mouseY);
        }
        graphics.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BARRIER), resetX, resetY);
    }
}
