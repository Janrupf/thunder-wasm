package net.janrupf.thunderwasm.assembler.generator;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.ClassFileEmitter;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.segment.ElementSegment;
import net.janrupf.thunderwasm.types.TableType;

public interface TableGenerator {
    /**
     * Add a table to the module.
     *
     * @param i       the index of the table
     * @param type    the type of the table
     * @param emitter the emitter to use
     * @throws WasmAssemblerException if an error occurs
     */
    void addTable(LargeArrayIndex i, TableType type, ClassFileEmitter emitter) throws WasmAssemblerException;

    /**
     * Add an element segment to the module.
     *
     * @param i       the index of the segment
     * @param segment the element segment
     * @param emitter the emitter to use
     * @throws WasmAssemblerException if an error occurs
     */
    void addElementSegment(LargeArrayIndex i, ElementSegment segment, ClassFileEmitter emitter) throws WasmAssemblerException;

    /**
     * Emit the table constructor.
     * <p>
     * The table constructor is responsible for initializing the table and setting the
     * field in the module if necessary.
     *
     * @param i       the index of the table
     * @param type    the type of the table
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitTableConstructor(LargeArrayIndex i, TableType type, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emit the element segment constructor.
     * <p>
     * The element segment constructor is responsible for initializing the element segment and setting the
     * field in the module if necessary.
     *
     * @param i          the index of the segment
     * @param segment    the element segment
     * @param initValues the initialization values
     * @param context    the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitElementSegmentConstructor(
            LargeArrayIndex i,
            ElementSegment segment,
            Object[] initValues,
            CodeEmitContext context
    ) throws WasmAssemblerException;

    /**
     * Emit a table get instruction.
     * <p>
     * This method expects the index to load to be on top of the stack.
     *
     * @param i       the index of the table
     * @param type    the type of the table
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitTableGet(LargeArrayIndex i, TableType type, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emit a table set instruction.
     * <p>
     * This method expects the index and then the element to store to be on top of the stack.
     *
     * @param i       the index of the table
     * @param type    the type of the table
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitTableSet(LargeArrayIndex i, TableType type, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emit a table copy instruction.
     * <p>
     * This method will only be called if {@link #canEmitCopyFor(ObjectType, ObjectType)} returns true.
     * <p>
     * This method expects the following stack top:
     * - source table reference (this could be an imported table too!)
     * - count
     * - source start index
     * - destination start index
     * - destination table reference (loaded by {@link #emitLoadTableReference(LargeArrayIndex, CodeEmitContext)})
     *
     * @param sourceType the source table type
     * @param targetType the target table type
     * @param context    the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    default void emitTableCopy(TableType sourceType, TableType targetType, CodeEmitContext context)
            throws WasmAssemblerException {
        throw new WasmAssemblerException("Optimized table copy not supported");
    }

    /**
     * Emit a table grow instruction.
     *
     * @param i       the index of the table
     * @param type    the type of the table
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitTableGrow(LargeArrayIndex i, TableType type, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emit a table size instruction.
     *
     * @param i       the index of the table
     * @param type    the type of the table
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitTableSize(LargeArrayIndex i, TableType type, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emit a table fill instruction.
     * <p>
     * This method expects the start index, value and count to be on top of the stack.
     *
     * @param i       the index of the table
     * @param type    the type of the table
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitTableFill(LargeArrayIndex i, TableType type, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emit a table init instruction.
     * <p>
     * This method will only be called if {@link #canEmitInitFor(ObjectType)} returns true.
     * <p>
     * This method expects the following stack top:
     * - count
     * - source start index
     * - destination start index
     * - destination table reference (could be imported!)
     *
     * @param tableType    the type of the table
     * @param elementIndex the index of the element segment
     * @param segment      the element segment
     * @param context      the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitTableInit(
            TableType tableType,
            LargeArrayIndex elementIndex,
            ElementSegment segment,
            CodeEmitContext context
    ) throws WasmAssemblerException;

    /**
     * Emit the code for loading the internal table reference.
     *
     * @param i       the index of the table
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitLoadTableReference(LargeArrayIndex i, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emit the code for loading an element from the element segment.
     * <p>
     * This method expects the index to load to be on top of the stack.
     *
     * @param i       the index of the element
     * @param segment the element segment
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitLoadElement(LargeArrayIndex i, ElementSegment segment, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Retrieves the underlying table type.
     *
     * @param i the index of the table
     * @return the table type
     */
    ObjectType getTableType(LargeArrayIndex i);

    /**
     * Determine whether an optimized copy instruction can be emitted for the given types.
     *
     * @param from the source table type
     * @param to   the destination table type
     * @return whether an optimized copy instruction can be emitted
     */
    default boolean canEmitCopyFor(ObjectType from, ObjectType to) {
        return false;
    }

    /**
     * Determine whether an optimized init instruction can be emitted for the given type.
     *
     * @param to the destination table type
     * @return whether an optimized init instruction can be emitted
     */
    default boolean canEmitInitFor(ObjectType to) {
        return false;
    }
}
