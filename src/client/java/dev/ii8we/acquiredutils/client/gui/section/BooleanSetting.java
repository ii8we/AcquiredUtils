package dev.ii8we.acquiredutils.client.gui.section;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class BooleanSetting implements Setting<Boolean> {
    private final String id;
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;
    private final boolean defaultValue;
    public BooleanSetting(String id, boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        this.id=id; this.defaultValue=defaultValue; this.getter=getter; this.setter=setter;
    }
    public String id(){return id;}
    public Boolean get(){return getter.get();}
    public void set(Boolean value){setter.accept(value);}
    public void reset(){setter.accept(defaultValue);}
}
