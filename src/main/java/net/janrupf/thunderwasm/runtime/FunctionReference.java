package net.janrupf.thunderwasm.runtime;

import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;
import net.janrupf.thunderwasm.types.ReferenceType;

public final class FunctionReference extends ElementReference {
    private final LinkedFunction function;

    public FunctionReference(LinkedFunction function) {
        this.function = function;
    }

    /**
     * Retrieves the linked function associated with this reference.
     *
     * @return the linked function
     */
    public LinkedFunction getFunction() {
        return function;
    }

    @Override
    public boolean isNull() {
        return function == null;
    }

    @Override
    public ReferenceType getType() {
        return ReferenceType.FUNCREF;
    }
}
