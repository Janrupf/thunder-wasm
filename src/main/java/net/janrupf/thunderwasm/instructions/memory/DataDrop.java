package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.WasmU32VariantInstruction;
import net.janrupf.thunderwasm.instructions.data.DataIndexData;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.segment.DataSegment;

import java.io.IOException;

public final class DataDrop extends WasmU32VariantInstruction<DataIndexData> {
    public static final DataDrop INSTANCE = new DataDrop();

    private DataDrop() {
        super("data.drop", (byte) 0xFC, 9);
    }

    @Override
    public DataIndexData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return DataIndexData.read(loader);
    }

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, DataIndexData data) throws WasmAssemblerException {
        final LargeArrayIndex index = data.toArrayIndex();
        final DataSegment segment = context.getLookups().requireDataSegment(index);
        
        return new ProcessedInstruction() {
            @Override
            public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
                context.getGenerators().getMemoryGenerator().emitDropData(index, segment, context);
            }
            
            @Override
            public void processOutputs(CodeEmitContext context) {
            }
        };
    }
}
