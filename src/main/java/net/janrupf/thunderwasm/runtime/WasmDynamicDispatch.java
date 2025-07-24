package net.janrupf.thunderwasm.runtime;

import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;

import java.lang.invoke.MethodHandle;

/**
 * Helper that is used by the generated code to dispatch call_indirect instructions.
 */
public final class WasmDynamicDispatch {
    private WasmDynamicDispatch() {
        throw new AssertionError("This is a helper class for generated code");
    }

    /**
     * Prepares a method handle for a call_indirect or call (via import) instruction.
     *
     * @param reference      the function reference to call
     * @return a method handle that can be invoked
     */
    public static MethodHandle prepareCallIndirect(FunctionReference reference) {
        LinkedFunction linkedFunction = reference.getFunction();

        if (linkedFunction == null) {
            throw new IllegalStateException("Attempted to invoke a null function");
        }

        MethodHandle methodHandle = linkedFunction.asMethodHandle();
        if (methodHandle == null) {
            throw new IllegalStateException("Method handle for linked function is null");
        }

        return methodHandle;
    }
}
