package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;

import java.util.List;

/**
 * Helper class for managing local variables in the context of code emission.
 * <p>
 * This very specifically exists because the conventions usually used by Java
 * don't really apply in the context of WASM code generation. For example, the
 * argument order can be different and the 'this' local is not always the first
 * argument.
 */
public final class LocalVariables {
    private final JavaLocal thisLocal;
    private final List<JavaLocal> staticLocals;

    public LocalVariables(
            JavaLocal thisLocal,
            List<JavaLocal> staticLocals
    ) {
        this.thisLocal = thisLocal;
        this.staticLocals = staticLocals;
    }

    /**
     * Retrieve the local variable representing 'this'.
     * <p>
     * Even if the method is static, this may return a local variable!
     *
     * @return the 'this' local variable, or null, if there is no 'this')
     */
    public JavaLocal getThis() {
        return thisLocal;
    }

    /**
     * Retrieve all static local variables.
     *
     * @return the list of all static local variables
     */
    public List<JavaLocal> getAllStatic() {
        return staticLocals;
    }

    /**
     * Retrieve the static local variable for the given index.
     * <p>
     * 'static' in this context means a local variable that is valid for the entire
     * duration of the function, not just for a specific block or scope.
     *
     * @param index the index of the static local, starting at 0
     * @return the static local variable for the given index
     * @throws WasmAssemblerException if the index is out of bounds
     */
    public JavaLocal getStatic(int index) throws WasmAssemblerException {
        if (index < 0 || index >= staticLocals.size()) {
            throw new WasmAssemblerException("Static local index out of bounds: " + index);
        }

        return staticLocals.get(index);
    }
}
