package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;

import java.util.*;

/**
 * Helper class for managing local variables in the context of code emission.
 * <p>
 * This very specifically exists because the conventions usually used by Java
 * don't really apply in the context of WASM code generation. For example, the
 * argument order can be different and the 'this' local is not always the first
 * argument.
 * <p>
 * This also helps tracking the usage of locals across nested blocks if block
 * splitting occurs.
 */
public final class LocalVariables {
    private final JavaLocal thisLocal;
    private final Map<Integer, JavaLocal> localsById;

    public LocalVariables(JavaLocal thisLocal) {
        this.localsById = new HashMap<>();

        this.thisLocal = thisLocal;
    }

    /**
     * Register a local that already exists and for which the ID is known.
     * <p>
     * This is required for being able to register the existing function argument locals.
     *
     * @param id the id of the local
     * @param local the existing local
     * @throws WasmAssemblerException if a local with the given ID is already registered
     */
    public void registerKnownLocal(int id, JavaLocal local) throws WasmAssemblerException {
        if (this.localsById.containsKey(id)) {
            throw new WasmAssemblerException("Local with id " + id + " is registered already");
        }

        this.localsById.put(id, local);
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
     * Require a local variable by its id.
     *
     * @param id the id of the local variable
     * @return the java local
     * @throws WasmAssemblerException if there is no local variable with this id
     */
    public JavaLocal requireById(int id) throws WasmAssemblerException {
        if (!this.localsById.containsKey(id)) {
            throw new WasmAssemblerException("Local with id " + id + " is not registered");
        }

        return  this.localsById.get(id);
    }
}
