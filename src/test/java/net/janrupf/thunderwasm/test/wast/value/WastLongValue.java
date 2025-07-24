package net.janrupf.thunderwasm.test.wast.value;

import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ValueType;

/**
 * WASM test value for i64.
 */
public final class WastLongValue extends WastValue {
    private final long value;

    public WastLongValue(long value) {
        this(value, false);
    }

    public WastLongValue(long value, boolean valueWildcard) {
        super(valueWildcard);
        this.value = value;
    }

    @Override
    public ValueType getType() {
        return NumberType.I64;
    }

    @Override
    public Long getValue() {
        return value;
    }
}