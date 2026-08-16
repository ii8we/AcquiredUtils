package dev.ii8we.acquiredutils.client.features;

public interface ClientFeature {
    String id();
    boolean isEnabled();
    void setEnabled(boolean enabled);
    void reset();
}
