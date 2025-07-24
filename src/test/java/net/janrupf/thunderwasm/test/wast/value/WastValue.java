package net.janrupf.thunderwasm.test.wast.value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.janrupf.thunderwasm.types.ValueType;

/**
 * Base class for WASM test values that combines a ValueType with its actual value.
 */
@JsonDeserialize(using = WastValueDeserializer.class)
public abstract class WastValue {
    private final boolean valueWildcard;

    protected WastValue(boolean valueWildcard) {
        this.valueWildcard = valueWildcard;
    }
    
    /**
     * Retrieves the WASM value type.
     *
     * @return the ValueType from the main Thunder WASM infrastructure
     */
    public abstract ValueType getType();
    
    /**
     * Retrieves the actual value as a Java object.
     * <p>
     * The return value has no meaning if {@link #isValueWildcard()} is true.
     *
     * @return the wrapped value (Integer, Long, Float, Double, byte[], or null for references)
     */
    public abstract Object getValue();
    
    /**
     * Creates a string representation of this value suitable for test output.
     *
     * @return string representation combining type and value
     */
    @Override
    public String toString() {
        return getType().getName() + ":" + getValue();
    }

    /**
     * Determines whether this value is to be treated as a wildcard.
     * <p>
     * This is mainly useful for return values from tests - if this is set
     * as a wildcard, no specific return value is expected, just the type
     * has to match.
     *
     * @return true if this is a value wildcard, false otherwise
     */
    public final boolean isValueWildcard() {
        return valueWildcard;
    }
}