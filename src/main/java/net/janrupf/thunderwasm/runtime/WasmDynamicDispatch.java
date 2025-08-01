package net.janrupf.thunderwasm.runtime;

import net.janrupf.thunderwasm.runtime.continuation.Continuation;
import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

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
     * @param reference    the function reference to call
     * @param continuation the continuation to use
     * @return a method handle that can be invoked
     */
    public static MethodHandle prepareCallIndirect(LinkedFunction reference, Continuation continuation) {
        if (reference == null) {
            throw new NullPointerException("Attempted to invoke a null function");
        }

        MethodHandle methodHandle = reference.asMethodHandle();
        if (methodHandle == null) {
            throw new IllegalStateException("Method handle for linked function is null");
        }

        if (reference.getContinuationArgumentIndex() != -1) {
            methodHandle = MethodHandles.insertArguments(
                    methodHandle,
                    reference.getContinuationArgumentIndex(),
                    continuation
            );
        }

        return methodHandle;
    }
}
