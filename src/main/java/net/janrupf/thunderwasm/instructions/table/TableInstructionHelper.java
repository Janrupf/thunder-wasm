package net.janrupf.thunderwasm.instructions.table;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.imports.TableImportDescription;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.types.TableType;

class TableInstructionHelper {
    private final FoundElement<TableType, TableImportDescription> element;
    private final CodeEmitContext emitContext;

    TableInstructionHelper(FoundElement<TableType, TableImportDescription> element, CodeEmitContext emitContext) {
        this.element = element;
        this.emitContext = emitContext;
    }

    /**
     * Retrieve the table type.
     *
     * @return the table type
     */
    public TableType getTableType() {
        if (element.isImport()) {
            return element.getImport().getDescription().getType();
        } else {
            return element.getElement();
        }
    }

    /**
     * Retrieves the backing Java type for the table.
     *
     * @return the backing Java type
     */
    public ObjectType getJavaTableType() {
        if (element.isImport()) {
            return emitContext.getGenerators().getImportGenerator().getTableType(element.getImport());
        } else {
            return emitContext.getGenerators().getTableGenerator().getTableType(element.getIndex());
        }
    }

    /**
     * Emit a table set operation.
     *
     * @throws WasmAssemblerException if an error occurs
     */
    public void emitTableSet() throws WasmAssemblerException {
        if (element.isImport()) {
            emitContext.getGenerators().getImportGenerator().emitTableSet(
                    element.getImport(),
                    emitContext
            );
        } else {
            emitContext.getGenerators().getTableGenerator().emitTableSet(
                    element.getIndex(),
                    element.getElement(),
                    emitContext
            );
        }
    }

    /**
     * Emit a table get operation.
     *
     * @throws WasmAssemblerException if an error occurs
     */
    public void emitTableGet() throws WasmAssemblerException {
        if (element.isImport()) {
            emitContext.getGenerators().getImportGenerator().emitTableGet(
                    element.getImport(),
                    emitContext
            );
        } else {
            emitContext.getGenerators().getTableGenerator().emitTableGet(
                    element.getIndex(),
                    element.getElement(),
                    emitContext
            );
        }
    }

    /**
     * Emit an operation loading the table reference.
     *
     * @throws WasmAssemblerException if an error occurs
     */
    public void emitLoadTableReference() throws WasmAssemblerException {
        if (element.isImport()) {
            emitContext.getGenerators().getImportGenerator().emitLoadTableReference(
                    element.getImport(),
                    emitContext
            );
        } else {
            emitContext.getGenerators().getTableGenerator().emitLoadTableReference(
                    element.getIndex(),
                    emitContext
            );
        }
    }

    /**
     * Emit an operation retrieving the table size.
     *
     * @throws WasmAssemblerException if an error occurs
     */
    public void emitTableSize() throws WasmAssemblerException {
        if (element.isImport()) {
            emitContext.getGenerators().getImportGenerator().emitTableSize(
                    element.getImport(),
                    emitContext
            );
        } else {
            emitContext.getGenerators().getTableGenerator().emitTableSize(
                    element.getIndex(),
                    element.getElement(),
                    emitContext
            );
        }
    }

    /**
     * Emit a table fill operation.
     *
     * @throws WasmAssemblerException if an error occurs
     */
    public void emitTableFill() throws WasmAssemblerException {
        if (element.isImport()) {
            emitContext.getGenerators().getImportGenerator().emitTableFill(
                    element.getImport(),
                    emitContext
            );
        } else {
            emitContext.getGenerators().getTableGenerator().emitTableFill(
                    element.getIndex(),
                    element.getElement(),
                    emitContext
            );
        }
    }
}
