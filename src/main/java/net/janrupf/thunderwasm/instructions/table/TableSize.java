package net.janrupf.thunderwasm.instructions.table;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.imports.TableImportDescription;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.data.TableIndexData;
import net.janrupf.thunderwasm.instructions.decoder.InstructionDecoder;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.TableType;

import java.io.IOException;

public final class TableSize extends WasmInstruction<TableIndexData> {
    public static final TableSize INSTANCE = new TableSize();

    private TableSize() {
        super("table.size", (byte) 0xFC);
    }

    @Override
    public InstructionDecoder getDecoder() {
        return InstructionDecoder.u32Variant(16, this);
    }

    @Override
    public TableIndexData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return TableIndexData.read(loader);
    }

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, TableIndexData data) throws WasmAssemblerException {
        final FoundElement<TableType, TableImportDescription> tableElement = context.getLookups().requireTable(data.toArrayIndex());
        
        return new ProcessedInstruction() {
            @Override
            public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
                if (tableElement.isImport()) {
                    context.getGenerators().getImportGenerator().emitTableSize(
                            tableElement.getImport(),
                            context
                    );
                } else {
                    context.getGenerators().getTableGenerator().emitTableSize(
                            tableElement.getIndex(),
                            tableElement.getElement(),
                            context
                    );
                }
            }
            
            @Override
            public void processOutputs(CodeEmitContext context) throws WasmAssemblerException {
                context.getFrameState().pushOperand(NumberType.I32);
            }
        };
    }
}
