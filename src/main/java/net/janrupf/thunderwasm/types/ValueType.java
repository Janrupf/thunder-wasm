package net.janrupf.thunderwasm.types;

import java.util.Objects;

/**
 * Base type for WASM value types
 */
public abstract class ValueType {
    private final String name;

    protected ValueType(String name) {
        this.name = name;
    }

    /**
     * Retrieves the name of the value type.
     *
     * @return the name of the value type
     */
    public final String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ValueType)) return false;
        ValueType valueType = (ValueType) o;
        return Objects.equals(name, valueType.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
