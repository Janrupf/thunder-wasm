package net.janrupf.thunderwasm.assembler.generator;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.ClassFileEmitter;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.imports.GlobalImportDescription;
import net.janrupf.thunderwasm.imports.Import;
import net.janrupf.thunderwasm.imports.TableImportDescription;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.segment.ElementSegment;
import net.janrupf.thunderwasm.types.TableType;

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
}
