package net.janrupf.thunderwasm.instructions.table;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.imports.TableImportDescription;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.data.TableIndexData;
import net.janrupf.thunderwasm.instructions.decoder.InstructionDecoder;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
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
    public void emitCode(CodeEmitContext context, TableIndexData data) throws WasmAssemblerException {
        LargeArrayIndex index = data.toArrayIndex();
        FoundElement<TableType, TableImportDescription> element = context.getLookups().requireTable(index);

        if (element.isImport()) {
            context.getGenerators().getImportGenerator().emitTableSize(
                    element.getImport(),
                    context
            );
        } else {
            context.getGenerators().getTableGenerator().emitTableSize(
                    element.getIndex(),
                    element.getElement(),
                    context
            );
        }
    }
}
