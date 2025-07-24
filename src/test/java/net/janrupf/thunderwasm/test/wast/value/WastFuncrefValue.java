package net.janrupf.thunderwasm.test.wast.value;

import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;

/**
 * WASM test value for function references.
 * <p>
 * This holds an unresolved function reference by its index.
 */
public final class WastFuncrefValue extends WastValue {
    private final Integer value;
    public WastFuncrefValue(Integer value) {
        this(value, false);
    }

    public WastFuncrefValue(Integer value, boolean valueWildcard) {
        super(valueWildcard);
        this.value = value;
    }
    

    @Override
    public ValueType getType() {
        return ReferenceType.FUNCREF;
    }
    
    @Override
    public Integer getValue() {
        return value;
    }
}