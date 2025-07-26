package net.janrupf.thunderwasm.instructions.table;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.imports.TableImportDescription;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.data.TableIndexData;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.TableType;

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
    public void emitCode(CodeEmitContext context, TableIndexData data) throws WasmAssemblerException {
        LargeArrayIndex index = data.toArrayIndex();
        FoundElement<TableType, TableImportDescription> element = context.getLookups().requireTable(index);

        if (element.isImport()) {
            context.getGenerators().getImportGenerator().emitTableSet(
                    element.getImport(),
                    context
            );
            context.getFrameState().popOperand(element.getImport().getDescription().getType().getElementType());
        } else {
            context.getGenerators().getTableGenerator().emitTableSet(
                    element.getIndex(),
                    element.getElement(),
                    context
            );
            context.getFrameState().popOperand(element.getElement().getElementType());
        }

        context.getFrameState().popOperand(NumberType.I32);
    }
}
