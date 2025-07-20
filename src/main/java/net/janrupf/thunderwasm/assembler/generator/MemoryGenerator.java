package net.janrupf.thunderwasm.assembler.generator;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.ClassFileEmitter;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemory;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryStore;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.segment.DataSegment;
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
     * Add a data segment to the module.
     *
     * @param i       the index of the data segment
     * @param segment the data segment to add
     * @param emitter the emitter to use
     * @throws WasmAssemblerException if the data segment could not be added
     */
    void addDataSegment(
            LargeArrayIndex i,
            DataSegment segment,
            ClassFileEmitter emitter
    ) throws WasmAssemblerException;

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
     * Emit the data segment constructor.
     * <p>
     * The data segment constructor is responsible for initializing the data segment and setting the
     * field in the module if necessary.
     *
     * @param i       the index of the data segment
     * @param segment the data segment to initialize
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitDataSegmentConstructor(
            LargeArrayIndex i,
            DataSegment segment,
            CodeEmitContext context
    ) throws WasmAssemblerException;

    /**
     * Emit a memory init instruction.
     * <p>
     * This method expects the following stack top:
     * - count
     * - source start index
     * - destination start index
     *
     * @param memoryIndex the index of the memory to initialize
     * @param type        the type of the memory
     * @param dataIndex   the index of the data segment to use
     * @param segment     the data segment to use for initialization
     * @param context     the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitMemoryInit(
            LargeArrayIndex memoryIndex,
            MemoryType type,
            LargeArrayIndex dataIndex,
            DataSegment segment,
            CodeEmitContext context
    ) throws WasmAssemblerException;

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

    /**
     * Emit a load instruction.
     * <p>
     * Expects the offset to be on top of the stack.
     *
     * @param i          the index of the memory
     * @param type       the type of the memory
     * @param numberType the type of value to load
     * @param memarg     additional memory access information
     * @param loadType   how to perform the load
     * @param context    the context to use
     * @throws WasmAssemblerException if the load could not be emitted
     */
    void emitLoad(
            LargeArrayIndex i,
            MemoryType type,
            NumberType numberType,
            PlainMemory.Memarg memarg,
            PlainMemoryLoad.LoadType loadType,
            CodeEmitContext context
    ) throws WasmAssemblerException;

    /**
     * Emit a memory copy instruction.
     * <p>
     * This method will only be called if {@link #canEmitCopyFor(ObjectType, ObjectType)} returns true.
     * <p>
     * This method expects the following stack top:
     * - source memory reference (this could be an imported memory too!)
     * - count
     * - source start index
     * - destination start index
     * - destination memory reference (loaded by {@link #emitLoadMemoryReference(LargeArrayIndex, CodeEmitContext)})
     *
     * @param context          the context to use
     * @param sourceMemoryType the source memory type
     * @param targetMemoryType the target memory type
     * @throws WasmAssemblerException if an error occurs
     */
    default void emitMemoryCopy(ObjectType sourceMemoryType, ObjectType targetMemoryType, CodeEmitContext context)
            throws WasmAssemblerException {
        throw new WasmAssemblerException("Optimized table copy not supported");
    }

    /**
     * Emit a drop data instruction.
     *
     * @param i       the index of the data segment to drop
     * @param segment the data segment to drop
     * @param context the context to use
     * @throws WasmAssemblerException if the drop data instruction could not be emitted
     */
    void emitDropData(
            LargeArrayIndex i,
            DataSegment segment,
            CodeEmitContext context
    ) throws WasmAssemblerException;

    /**
     * Emit the code for loading the internal memory reference.
     *
     * @param i       the index of the memory
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitLoadMemoryReference(LargeArrayIndex i, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Retrieves the underlying memory type.
     *
     * @param i the index of the table
     * @return the table type
     */
    ObjectType getMemoryType(LargeArrayIndex i);

    /**
     * Determine whether an optimized copy instruction can be emitted for the given types.
     *
     * @param from the source memory type
     * @param to   the destination memory type
     * @return whether an optimized copy instruction can be emitted
     */
    default boolean canEmitCopyFor(ObjectType from, ObjectType to) {
        return false;
    }
}
