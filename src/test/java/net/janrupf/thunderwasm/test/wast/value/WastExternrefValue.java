package net.janrupf.thunderwasm.test.wast.value;

import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;

/**
 * WASM test value for extern references.
 */
public final class WastExternrefValue extends WastValue {
    private final Object value;

    public WastExternrefValue(Object value) {
        this(value, false);
    }

    public WastExternrefValue(Object value, boolean valueWildcard) {
        super(valueWildcard);
        this.value = value;
    }

    @Override
    public ValueType getType() {
        return ReferenceType.EXTERNREF;
    }

    @Override
    public Object getValue() {
        return value;
    }
}
