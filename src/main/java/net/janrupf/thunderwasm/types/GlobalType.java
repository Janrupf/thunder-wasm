package net.janrupf.thunderwasm.types;

/**
 * Represents a global type.
 */
public final class GlobalType {
    private final Mutability mutability;
    private final ValueType valueType;

    public GlobalType(Mutability mutability, ValueType valueType) {
        this.mutability = mutability;
        this.valueType = valueType;
    }

    /**
     * Retrieves the mutability of the global.
     *
     * @return the mutability
     */
    public Mutability getMutability() {
        return mutability;
    }

    /**
     * Retrieves the value type of the global.
     *
     * @return the value type
     */
    public ValueType getValueType() {
        return valueType;
    }

    /**
     * Mutability of the global.
     */
    public enum Mutability {
        /**
         * The global is mutable.
         */
        VAR,

        /**
         * The global is immutable.
         */
        CONST
    }
}
