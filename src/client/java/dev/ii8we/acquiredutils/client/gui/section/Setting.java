package dev.ii8we.acquiredutils.client.gui.section;

public interface Setting<T> {
    String id();
    T get();
    void set(T value);
    void reset();
}
