package net.janrupf.thunderwasm.test.wast.value;

import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ValueType;

/**
 * WASM test value for i32.
 */
public final class WastIntValue extends WastValue {
    private final int value;
    
    public WastIntValue(int value) {
        this(value, false);
    }

    public WastIntValue(int value, boolean valueWildcard) {
        super(valueWildcard);
        this.value = value;
    }
    
    @Override
    public ValueType getType() {
        return NumberType.I32;
    }
    
    @Override
    public Integer getValue() {
        return value;
    }
}