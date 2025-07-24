package net.janrupf.thunderwasm.runtime;

import net.janrupf.thunderwasm.types.ReferenceType;

public abstract class ElementReference {
    /**
     * Determines whether the reference is null.
     *
     * @return whether the reference is null
     */
    public abstract boolean isNull();

    /**
     * Retrieves the type of the reference.
     *
     * @return the type of the reference
     */
    public abstract ReferenceType getType();

    /**
     * Creates a new null reference of the given type.
     *
     * @param type the type of the reference
     * @return the null reference
     */
    public static ElementReference nullOf(ReferenceType type) {
        if (type.equals(ReferenceType.FUNCREF)) {
            return UnresolvedFunctionReference.ofNull();
        } else if (type.equals(ReferenceType.EXTERNREF)) {
            return ExternReference.ofNull();
        }

        throw new IllegalArgumentException("Unsupported reference type: " + type);
    }
}
