package dev.ii8we.acquiredutils.client.overlay;

public record OverlayPosition(float x, float y) {
    public OverlayPosition {
        x=Math.max(0f,Math.min(1f,x)); y=Math.max(0f,Math.min(1f,y));
    }
}
