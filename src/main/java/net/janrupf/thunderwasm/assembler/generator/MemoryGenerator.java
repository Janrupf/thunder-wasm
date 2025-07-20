package net.janrupf.thunderwasm.assembler.generator;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.ClassFileEmitter;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemory;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryStore;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.MemoryType;
import net.janrupf.thunderwasm.types.NumberType;

public interface MemoryGenerator {
    /**
     * Add a memory to the module.
     *
     * @param i       the index of the memory to add
     * @param type    the type of the memory
     * @param emitter the emitter to use
     * @throws WasmAssemblerException if the memory could not be added
     */
    void addMemory(LargeArrayIndex i, MemoryType type, ClassFileEmitter emitter) throws WasmAssemblerException;

    /**
     * Emit the memory constructor.
     * <p>
     * The memory constructor is responsible for initializing the memory and setting the
     * memory field in the module if necessary.
     *
     * @param i       the index of the memory
     * @param type    the type of the memory
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitMemoryConstructor(LargeArrayIndex i, MemoryType type, CodeEmitContext context)
            throws WasmAssemblerException;

    /**
     * Emit a store instruction.
     * <p>
     * Expects the value to be on top of the stack followed by the offset.
     *
     * @param i          the index of the memory
     * @param type       the type of the memory
     * @param numberType the type of value on the stack
     * @param memarg     additional memory access information
     * @param storeType  how to perform the store
     * @param context    the context to use
     * @throws WasmAssemblerException if the store could not be emitted
     */
    void emitStore(
            LargeArrayIndex i,
            MemoryType type,
            NumberType numberType,
            PlainMemory.Memarg memarg,
            PlainMemoryStore.StoreType storeType,
            CodeEmitContext context
    ) throws WasmAssemblerException;
}
