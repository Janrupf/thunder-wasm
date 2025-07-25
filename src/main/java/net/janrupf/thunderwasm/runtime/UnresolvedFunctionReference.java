package net.janrupf.thunderwasm.runtime;

import net.janrupf.thunderwasm.types.ReferenceType;

/**
 * Represents a reference to a function that has not yet been resolved.
 * This is used in scenarios where the function index is known, but the
 * actual function has not been linked or defined yet, mostly during
 * constant evaluation or early stages of module assembly.
 */
public final class UnresolvedFunctionReference {
    private final Integer functionIndex;

    private UnresolvedFunctionReference(Integer functionIndex) {
        this.functionIndex = functionIndex;
    }

    public boolean isNull() {
        return functionIndex == null;
    }

    /**
     * Retrieves the index of the function.
     *
     * @return the index of the function
     */
    public int getFunctionIndex() {
        return functionIndex;
    }

    /**
     * Creates a new non-null function reference.
     *
     * @param functionIndex the index of the function
     * @return the new function reference
     */
    public static UnresolvedFunctionReference of(int functionIndex) {
        return new UnresolvedFunctionReference(functionIndex);
    }

    /**
     * Creates a new null function reference.
     *
     * @return the new null function reference
     */
    public static UnresolvedFunctionReference ofNull() {
        return new UnresolvedFunctionReference(null);
    }
}
