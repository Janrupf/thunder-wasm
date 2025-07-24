package net.janrupf.thunderwasm.assembler.generator;

import jdk.internal.classfile.ClassModel;
import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.ClassEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.ClassFileEmitter;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.exports.GlobalExportDescription;
import net.janrupf.thunderwasm.imports.*;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemory;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryStore;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.segment.DataSegment;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.MemoryType;
import net.janrupf.thunderwasm.types.NumberType;

import java.lang.reflect.Type;

public interface ImportGenerator {
    /**
     * Retrieves the type of the linker to be passed to the module constructor.
     *
     * @return the linker type
     */
    ObjectType getLinkerType();

    /**
     * Adds an import to the module.
     *
     * @param im      the import to add
     * @param emitter the emitter to add the import to
     * @throws WasmAssemblerException if the import could not be added
     */
    void addImport(Import<?> im, ClassFileEmitter emitter) throws WasmAssemblerException;

    /**
     * Emits the code to link an import.
     * <p>
     * This method will be invoked to generate code with the linker on top of the stack.
     *
     * @param im      the import to link
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void emitLinkImport(Import<?> im, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emits the code to get an imported global variable.
     *
     * @param im      the global import
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void emitGetGlobal(Import<GlobalImportDescription> im, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emits the code to set an imported global variable.
     * <p>
     * This method will be invoked to generate code with the new global value on top of the stack.
     *
     * @param im      the global import
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void emitSetGlobal(Import<GlobalImportDescription> im, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emit a table get instruction.
     * <p>
     * This method expects the index to load to be on top of the stack.
     *
     * @param im      the table import
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitTableGet(Import<TableImportDescription> im, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emit a table set instruction.
     * <p>
     * This method expects the index and then the element to store to be on top of the stack.
     *
     * @param im      the table import
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitTableSet(Import<TableImportDescription> im, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emit a table grow instruction.
     *
     * @param im      the table import
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitTableGrow(Import<TableImportDescription> im, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emit a table size instruction.
     *
     * @param im      the table import
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitTableSize(Import<TableImportDescription> im, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emit a table fill instruction.
     * <p>
     * This method expects the start index, value and count to be on top of the stack.
     *
     * @param im      the table import
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitTableFill(Import<TableImportDescription> im, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emit the code for loading the internal table reference.
     *
     * @param im      the table import
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitLoadTableReference(Import<TableImportDescription> im, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Retrieves the underlying table type.
     *
     * @param im the table import
     * @return the table type
     */
    ObjectType getTableType(Import<TableImportDescription> im);


    /**
     * Emit a store instruction.
     * <p>
     * Expects the value to be on top of the stack followed by the offset.
     *
     * @param im         the memory import
     * @param numberType the type of value on the stack
     * @param memarg     additional memory access information
     * @param storeType  how to perform the store
     * @param context    the context to use
     * @throws WasmAssemblerException if the store could not be emitted
     */
    void emitMemoryStore(
            Import<MemoryImportDescription> im,
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
     * @param im         the memory import
     * @param numberType the type of value to load
     * @param memarg     additional memory access information
     * @param loadType   how to perform the load
     * @param context    the context to use
     * @throws WasmAssemblerException if the load could not be emitted
     */
    void emitMemoryLoad(
            Import<MemoryImportDescription> im,
            NumberType numberType,
            PlainMemory.Memarg memarg,
            PlainMemoryLoad.LoadType loadType,
            CodeEmitContext context
    ) throws WasmAssemblerException;

    /**
     * Emit a grow instruction.
     * <p>
     * Expects the amount of pages to grow by to be on top of the stack.
     *
     * @param im      the memory import
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitMemoryGrow(Import<MemoryImportDescription> im, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emit a size instruction.
     *
     * @param im      the memory import
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitMemorySize(Import<MemoryImportDescription> im, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emit a memory init instruction.
     * <p>
     * This method expects the following stack top:
     * - count
     * - source start index
     * - destination start index
     *
     * @param im        the memory import
     * @param dataIndex the index of the data segment to use
     * @param segment   the data segment to use for initialization
     * @param context   the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitMemoryInit(
            Import<MemoryImportDescription> im,
            LargeArrayIndex dataIndex,
            DataSegment segment,
            CodeEmitContext context
    ) throws WasmAssemblerException;

    /**
     * Emit the code for loading the internal memory reference.
     *
     * @param im      the memory import
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitLoadMemoryReference(Import<MemoryImportDescription> im, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emit the code to invoke a module local function by its index.
     *
     * @param im      the function import
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void emitInvokeFunction(Import<TypeImportDescription> im, CodeEmitContext context)
            throws WasmAssemblerException;

    /**
     * Emit the code to load a function reference.
     *
     * @param im      the function import
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void emitLoadFunctionReference(Import<TypeImportDescription> im, CodeEmitContext context)
            throws WasmAssemblerException;

    /**
     * Make an imported function exportable.
     *
     * @param im      the imported function to make exportable
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void makeFunctionExportable(Import<TypeImportDescription> im, ClassEmitContext context)
            throws WasmAssemblerException;

    /**
     * Emit the code to load an export of a function import.
     *
     * @param im      the imported function to export
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitLoadFunctionExport(Import<TypeImportDescription> im, CodeEmitContext context)
            throws WasmAssemblerException;

    /**
     * Make an imported global exportable.
     *
     * @param im      the imported function to make exportable
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void makeGlobalExportable(Import<GlobalImportDescription> im, ClassEmitContext context)
            throws WasmAssemblerException;

    /**
     * Emit the code to load an export of a global import.
     *
     * @param im      the imported global to export
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitLoadGlobalExport(Import<GlobalImportDescription> im, CodeEmitContext context)
            throws WasmAssemblerException;

    /**
     * Make an imported memory exportable.
     *
     * @param im      the imported memory to make exportable
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void makeMemoryExportable(Import<MemoryImportDescription> im, ClassEmitContext context)
            throws WasmAssemblerException;

    /**
     * Emit the code to load an export of a memory import.
     *
     * @param im      the imported memory to export
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitLoadMemoryExport(Import<MemoryImportDescription> im, CodeEmitContext context)
            throws WasmAssemblerException;

    /**
     * Make an imported table exportable.
     *
     * @param im      the imported table to make exportable
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void makeTableExportable(Import<TableImportDescription> im, ClassEmitContext context)
            throws WasmAssemblerException;

    /**
     * Emit the code to load an export of a table import.
     *
     * @param im      the imported table to export
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitLoadTableExport(Import<TableImportDescription> im, CodeEmitContext context)
            throws WasmAssemblerException;


    /**
     * Retrieves the underlying memory type.
     *
     * @param im the memory import
     * @return the table type
     */
    ObjectType getMemoryType(Import<MemoryImportDescription> im);
}
