package net.janrupf.thunderwasm.eval;

import net.janrupf.thunderwasm.types.ValueType;

/**
 * Represents an entry on the evaluation stack.
 */
public final class StackEntry {
    private final ValueType type;
    private final Object value;

    public StackEntry(ValueType type, Object value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Retrieves the type of the stack entry.
     *
     * @return the type
     */
    public ValueType getType() {
        return type;
    }

    /**
     * Retrieves the value of the stack entry.
     *
     * @return the value
     */
    public Object getValue() {
        return value;
    }
}
