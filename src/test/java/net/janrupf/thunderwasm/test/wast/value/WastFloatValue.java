package net.janrupf.thunderwasm.test.wast.value;

import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ValueType;

/**
 * WASM test value for f32.
 */
public final class WastFloatValue extends WastValue {
    private final float value;

    public WastFloatValue(float value) {
        this(value, false);
    }

    public WastFloatValue(float value, boolean valueWildcard) {
        super(valueWildcard);
        this.value = value;
    }

    @Override
    public ValueType getType() {
        return NumberType.F32;
    }

    @Override
    public Float getValue() {
        return value;
    }
}
