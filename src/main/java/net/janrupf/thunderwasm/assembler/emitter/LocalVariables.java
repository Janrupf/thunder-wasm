package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.ArrayType;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final CodeEmitter codeEmitter;
    private final JavaLocal thisLocal;
    private final Map<Integer, LocalUsage> usage;
    private final LocalVariables parent;

    public LocalVariables(
            CodeEmitter codeEmitter,
            JavaLocal thisLocal
    ) {
        this(codeEmitter, thisLocal, null);
    }

    public LocalVariables(
            CodeEmitter codeEmitter,
            JavaLocal thisLocal,
            LocalVariables parent
    ) {
        this.codeEmitter = codeEmitter;
        this.usage = new HashMap<>();


        this.thisLocal = thisLocal;
        this.parent = parent;
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
        if (this.usage.containsKey(id)) {
            throw new WasmAssemblerException("Local with id " + id + " is registered already");
        }

        this.usage.put(id, new LocalUsage(local, true, true));
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
     * Mark a local as being read.
     *
     * @param id the id of the local being read
     * @param type the type of the local
     * @return the local
     * @throws WasmAssemblerException if the local could not be allocated
     */
    public JavaLocal readLocal(int id, JavaType type) throws WasmAssemblerException {
        LocalUsage usage = useLocal(id, type);
        usage.markRead();

        return usage.getLocal();
    }

    /**
     * Mark a local as being written.
     *
     * @param id the id of the local being written
     * @param type the type of the local
     * @return the local
     * @throws WasmAssemblerException if the local could not be allocated
     */
    public JavaLocal writeLocal(int id, JavaType type) throws WasmAssemblerException {
        LocalUsage usage = useLocal(id, type);
        usage.markWrite();

        return usage.getLocal();
    }

    /**
     * Ensure that the local usage for a given local is recorded.
     * <p>
     * This also recursively populates the usage upwards.
     *
     * @param id the id of the local to get a usage tracker for
     * @param type the type of the local
     * @return the user tracker for the given local id
     * @throws WasmAssemblerException if the local could not be allocated
     */
    private LocalUsage useLocal(int id, JavaType type) throws WasmAssemblerException {
        if (!this.usage.containsKey(id)) {
            this.usage.put(id, new LocalUsage(this.codeEmitter.allocateLocal(type)));
        }

        if (parent != null) {
            parent.useLocal(id, type);
        }

        LocalUsage usage = this.usage.get(id);
        JavaType existingType = usage.getLocal().getType();
        if (!existingType.equals(type) && !(type instanceof ObjectType && existingType instanceof ObjectType) ||
                (existingType instanceof ArrayType && !(type instanceof ArrayType))
        ) {
            throw new WasmAssemblerException("Local type mismatch detected, expected " + existingType + " but found " + type);
        }

        return usage;
    }

    /**
     * Tracker for how a local is used inside a function.
     */
    public static final class LocalUsage {
        private final JavaLocal local;
        private boolean read;
        private boolean write;

        private LocalUsage(JavaLocal local) {
            this(local, false, false);
        }

        private LocalUsage(JavaLocal local, boolean read, boolean write) {
            this.local = local;
            this.read = read;
            this.write = write;
        }

        /**
         * Retrieve the actual local that is being tracked.
         *
         * @return the local that is being tracked
         */
        public JavaLocal getLocal() {
            return local;
        }

        /**
         * Mark the local as being read.
         */
        public void markRead() {
            this.read = true;
        }

        /**
         * Mark the local as being written.
         */
        public void markWrite() {
            this.write = true;
        }

        /**
         * Determines whether the local was read.
         *
         * @return true if the local was read, false otherwise
         */
        public boolean isRead() {
            return this.read;
        }

        /**
         * Determines whether the local was written.
         *
         * @return true if the local was written, false otherwise
         */
        public boolean isWritten() {
            return this.write;
        }
    }
}
