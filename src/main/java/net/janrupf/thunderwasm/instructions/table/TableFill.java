package net.janrupf.thunderwasm.instructions.table;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CommonBytecodeGenerator;
import net.janrupf.thunderwasm.assembler.emitter.InvokeType;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.imports.TableImportDescription;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.data.TableIndexData;
import net.janrupf.thunderwasm.instructions.decoder.InstructionDecoder;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.runtime.BoundsChecks;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.TableType;

import java.io.IOException;

public final class TableFill extends WasmInstruction<TableIndexData> {
    public static final TableFill INSTANCE = new TableFill();

    private TableFill() {
        super("table.fill", (byte) 0xFC);
    }

    @Override
    public InstructionDecoder getDecoder() {
        return InstructionDecoder.u32Variant(17, this);
    }

    @Override
    public TableIndexData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return TableIndexData.read(loader);
    }

    @Override
    public void emitCode(CodeEmitContext context, TableIndexData data) throws WasmAssemblerException {
        LargeArrayIndex index = data.toArrayIndex();
        FoundElement<TableType, TableImportDescription> element = context.getLookups().requireTable(index);

        TableInstructionHelper helper = new TableInstructionHelper(element, context);

        context.getFrameState().popOperand(NumberType.I32);
        context.getFrameState().popOperand(helper.getTableType().getElementType());
        context.getFrameState().popOperand(NumberType.I32);

        if (context.getConfiguration().atomicBoundsChecksEnabled()) {
            CommonBytecodeGenerator.emitPrepareWriteBoundsCheck(context.getEmitter());
            helper.emitTableSize();

            context.getEmitter().invoke(
                    ObjectType.of(BoundsChecks.class),
                    "checkTableBulkWrite",
                    new JavaType[]{ PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT },
                    PrimitiveType.VOID,
                    InvokeType.STATIC,
                    false
            );
        }

        helper.emitTableFill();
    }
}
