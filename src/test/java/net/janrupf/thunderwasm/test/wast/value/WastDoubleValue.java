package net.janrupf.thunderwasm.test.wast.value;

import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ValueType;

/**
 * WASM test value for f64.
 */
public final class WastDoubleValue extends WastValue {
    private final double value;
    
    public WastDoubleValue(double value) {
        this(value, false);
    }

    public WastDoubleValue(double value, boolean valueWildcard) {
        super(valueWildcard);
        this.value = value;
    }
    
    @Override
    public ValueType getType() {
        return NumberType.F64;
    }
    
    @Override
    public Double getValue() {
        return value;
    }
}
