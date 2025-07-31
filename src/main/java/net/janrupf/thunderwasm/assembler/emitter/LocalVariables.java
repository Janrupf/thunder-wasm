package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;

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
    private final JavaLocal heapLocals;
    private final Map<Integer, JavaLocal> localsById;
    private final Map<Integer, HeapLocal> heapLocalsById;

    public LocalVariables(JavaLocal thisLocal, JavaLocal heapLocals) {
        this.thisLocal = thisLocal;
        this.heapLocals = heapLocals;

        this.localsById = new HashMap<>();
        this.heapLocalsById = new HashMap<>();
    }

    /**
     * Register a local that already exists and for which the ID is known.
     * <p>
     * This is required for being able to register the existing function argument locals.
     *
     * @param id    the id of the local
     * @param local the existing local
     * @throws WasmAssemblerException if a local with the given ID is already registered
     */
    public void registerKnownLocal(int id, JavaLocal local) throws WasmAssemblerException {
        if (getType(id) != LocalType.UNDEFINED) {
            throw new WasmAssemblerException("Local with id " + id + " is registered already");
        }

        this.localsById.put(id, local);
    }

    /**
     * Register a local that already exists and has been allocated on the heap.
     *
     * @param id    the id of the local
     * @param type  the java type of the local
     * @param index the local index in the backing storage
     * @throws WasmAssemblerException if a local with the given ID is already registered
     */
    public void registerKnownHeapLocal(int id, JavaType type, int index) throws WasmAssemblerException {
        if (heapLocals == null) {
            throw new WasmAssemblerException("Can't register a heap local if no heap local storage is provided");
        }

        if (getType(id) != LocalType.UNDEFINED) {
            throw new WasmAssemblerException("Local with id " + id + " is registered already");
        }

        this.heapLocalsById.put(id, new HeapLocal(type, index));
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
     * Retrieve the local variable that holds the heap local storage.
     *
     * @return the heap locals storage, or null, if not using heap locals
     */
    public JavaLocal getHeapLocals() {
        return heapLocals;
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

        return this.localsById.get(id);
    }

    /**
     * Require a heap local by its id.
     *
     * @param id the id of the local variable
     * @return the heap local
     * @throws WasmAssemblerException if there is no heap local variable with this id
     */
    public HeapLocal requireHeapById(int id) throws WasmAssemblerException {
        if (!this.heapLocalsById.containsKey(id)) {
            throw new WasmAssemblerException("Heap local with id " + id + " is not registered");
        }

        return this.heapLocalsById.get(id);
    }

    /**
     * Look up the type of a local by its id.
     *
     * @param id the id of the local
     * @return the local type
     */
    public LocalType getType(int id) {
        if (this.heapLocalsById.containsKey(id)) {
            return LocalType.HEAP;
        } else if (this.localsById.containsKey(id)) {
            return LocalType.LOCAL;
        }

        return LocalType.UNDEFINED;
    }

    public static class HeapLocal {
        private final JavaType type;
        private final int index;

        public HeapLocal(JavaType type, int index) {
            this.type = type;
            this.index = index;
        }

        public JavaType getType() {
            return type;
        }

        public int getIndex() {
            return index;
        }
    }

    public enum LocalType {
        /**
         * A local that has been allocated into a Java local.
         */
        LOCAL,

        /**
         * A local that has been moved to the heap.
         */
        HEAP,

        /**
         * A local that has not been defined (is unknown)
         */
        UNDEFINED
    }
}
