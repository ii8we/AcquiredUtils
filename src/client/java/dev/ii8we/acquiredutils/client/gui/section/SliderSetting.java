package dev.ii8we.acquiredutils.client.gui.section;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SliderSetting implements Setting<Float> {
    private final String id; private final float defaultValue; private final Supplier<Float> getter; private final Consumer<Float> setter;
    public SliderSetting(String id, float defaultValue, Supplier<Float> getter, Consumer<Float> setter){this.id=id;this.defaultValue=defaultValue;this.getter=getter;this.setter=setter;}
    public String id(){return id;} public Float get(){return getter.get();} public void set(Float value){setter.accept(value);} public void reset(){setter.accept(defaultValue);}
}
