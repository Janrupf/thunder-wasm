package net.janrupf.thunderwasm.instructions;

import net.janrupf.thunderwasm.types.ValueType;

public final class Local {
    private final int count;
    private final ValueType type;

    public Local(int count, ValueType type) {
        this.count = count;
        this.type = type;
    }

    /**
     * Retrieves the count of the local variables of the same type.
     *
     * @return the count of the local variables of the same type
     */
    public int getCount() {
        return count;
    }

    /**
     * Retrieves the type of the local variables.
     *
     * @return the type of the local variables
     */
    public ValueType getType() {
        return type;
    }
}
