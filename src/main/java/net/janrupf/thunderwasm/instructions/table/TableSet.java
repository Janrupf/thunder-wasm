package net.janrupf.thunderwasm.instructions.table;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.imports.TableImportDescription;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.data.TableIndexData;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.TableType;
import net.janrupf.thunderwasm.types.ValueType;

import java.io.IOException;

public final class TableSet extends WasmInstruction<TableIndexData> {
    public static final TableSet INSTANCE = new TableSet();

    private TableSet() {
        super("table.set", (byte) 0x26);
    }

    @Override
    public TableIndexData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return TableIndexData.read(loader);
    }

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, TableIndexData data) throws WasmAssemblerException {
        final FoundElement<TableType, TableImportDescription> tableElement = context.getLookups().requireTable(data.toArrayIndex());
        final ValueType elementType = tableElement.isImport() 
            ? tableElement.getImport().getDescription().getType().getElementType()
            : tableElement.getElement().getElementType();
        context.getFrameState().popOperand(elementType);
        context.getFrameState().popOperand(NumberType.I32);
        
        return new ProcessedInstruction() {
            @Override
            public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
                if (tableElement.isImport()) {
                    context.getGenerators().getImportGenerator().emitTableSet(
                            tableElement.getImport(),
                            context
                    );
                } else {
                    context.getGenerators().getTableGenerator().emitTableSet(
                            tableElement.getIndex(),
                            tableElement.getElement(),
                            context
                    );
                }
            }
            
            @Override
            public void processOutputs(CodeEmitContext context) {
            }
        };
    }
}
