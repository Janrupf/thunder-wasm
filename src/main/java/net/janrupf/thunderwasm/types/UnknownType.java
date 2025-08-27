package net.janrupf.thunderwasm.types;

/**
 * Represents an unknown type on the polymorphic stack.
 * Used when code is unreachable to allow validation to continue
 * while maintaining proper stack semantics.
 * 
 * In unreachable code, the stack becomes "polymorphic" meaning
 * any type can be popped or pushed as this unknown type.
 */
public final class UnknownType extends ValueType {
    /**
     * Singleton instance of the unknown type.
     */
    public static final UnknownType UNKNOWN = new UnknownType();
    
    private UnknownType() {
        super("unknown");
    }
    
    /**
     * Check if this unknown type is compatible with another value type.
     * The unknown type is compatible with any type (including itself).
     * 
     * @param other the other value type to check compatibility with
     * @return true (unknown type is always compatible)
     */
    public boolean isCompatibleWith(ValueType other) {
        return true;
    }
    
    /**
     * Check if any value type is compatible with the unknown type.
     * Any type is compatible with the unknown type.
     * 
     * @param type the value type to check compatibility with
     * @return true (any type is compatible with unknown)
     */
    public static boolean isAnyTypeCompatible(ValueType type) {
        return true;
    }
}