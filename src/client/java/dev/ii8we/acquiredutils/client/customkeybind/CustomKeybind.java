package dev.ii8we.acquiredutils.client.customkeybind;

import java.util.UUID;

public final class CustomKeybind {
    private final String id;
    private String name;
    private int keyCode;
    private boolean control;
    private boolean alt;
    private boolean shift;
    private String actionText;

    public CustomKeybind(String name, int keyCode, String actionText) {
        this(UUID.randomUUID().toString(), name, keyCode, false, false, false, actionText);
    }

    public CustomKeybind(String name, int keyCode, boolean control, boolean alt, boolean shift, String actionText) {
        this(UUID.randomUUID().toString(), name, keyCode, control, alt, shift, actionText);
    }

    public CustomKeybind(String id, String name, int keyCode, String actionText) {
        this(id, name, keyCode, false, false, false, actionText);
    }

    public CustomKeybind(String id, String name, int keyCode, boolean control, boolean alt, boolean shift, String actionText) {
        this.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        this.name = name == null ? "" : name;
        this.keyCode = keyCode;
        this.control = control;
        this.alt = alt;
        this.shift = shift;
        this.actionText = actionText == null ? "" : actionText;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public boolean isControl() {
        return control;
    }

    public boolean isAlt() {
        return alt;
    }

    public boolean isShift() {
        return shift;
    }

    public void setModifiers(boolean control, boolean alt, boolean shift) {
        this.control = control;
        this.alt = alt;
        this.shift = shift;
    }

    public String getActionText() {
        return actionText;
    }

    public void setActionText(String actionText) {
        this.actionText = actionText == null ? "" : actionText;
    }

    public CustomKeybind copy() {
        return new CustomKeybind(id, name, keyCode, control, alt, shift, actionText);
    }
}
