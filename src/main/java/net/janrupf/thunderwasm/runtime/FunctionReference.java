package net.janrupf.thunderwasm.runtime;

import net.janrupf.thunderwasm.types.ReferenceType;

public final class FunctionReference extends ElementReference {
    private final Integer functionIndex;

    private FunctionReference(Integer functionIndex) {
        this.functionIndex = functionIndex;
    }

    @Override
    public boolean isNull() {
        return functionIndex == null;
    }

    @Override
    public ReferenceType getType() {
        return ReferenceType.FUNCREF;
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
    public static FunctionReference of(int functionIndex) {
        return new FunctionReference(functionIndex);
    }

    /**
     * Creates a new null function reference.
     *
     * @return the new null function reference
     */
    public static FunctionReference ofNull() {
        return new FunctionReference(null);
    }
}
