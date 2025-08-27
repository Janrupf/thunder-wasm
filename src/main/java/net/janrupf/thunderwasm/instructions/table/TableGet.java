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

public final class TableGet extends WasmInstruction<TableIndexData> {
    public static final TableGet INSTANCE = new TableGet();

    private TableGet() {
        super("table.get", (byte) 0x25);
    }

    @Override
    public TableIndexData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return TableIndexData.read(loader);
    }

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, TableIndexData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I32);
        final FoundElement<TableType, TableImportDescription> tableElement = context.getLookups().requireTable(data.toArrayIndex());
        final ValueType elementType = tableElement.isImport() 
            ? tableElement.getImport().getDescription().getType().getElementType()
            : tableElement.getElement().getElementType();
        
        return new ProcessedInstruction() {
            @Override
            public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
                if (tableElement.isImport()) {
                    context.getGenerators().getImportGenerator().emitTableGet(
                            tableElement.getImport(),
                            context
                    );
                } else {
                    context.getGenerators().getTableGenerator().emitTableGet(
                            tableElement.getIndex(),
                            tableElement.getElement(),
                            context
                    );
                }
            }
            
            @Override
            public void processOutputs(CodeEmitContext context) throws WasmAssemblerException {
                context.getFrameState().pushOperand(elementType);
            }
        };
    }
}
