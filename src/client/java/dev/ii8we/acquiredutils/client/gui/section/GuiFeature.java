package dev.ii8we.acquiredutils.client.gui.section;

import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A top-level feature with an optional enabled state and child settings.
 *
 * Child rows belong to this feature and are only exposed while the parent
 * feature is enabled. Keeping the relationship here prevents feature-specific
 * GUI code from becoming a pile of unrelated rows in the section.
 */
public record GuiFeature(
    String id,
    String labelKey,
    String descKey,
    BooleanSupplier enabled,
    Consumer<Boolean> toggle,
    GuiRow.ControlFactory controlFactory,
    List<GuiRow> children
) {
    public GuiFeature {
        children = children == null ? Collections.emptyList() : List.copyOf(children);
    }

    public boolean isEnabled() {
        return enabled != null && enabled.getAsBoolean();
    }
}
