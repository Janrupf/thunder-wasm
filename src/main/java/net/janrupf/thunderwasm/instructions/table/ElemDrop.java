package net.janrupf.thunderwasm.instructions.table;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.WasmU32VariantInstruction;
import net.janrupf.thunderwasm.instructions.data.ElementIndexData;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.section.segment.ElementSegment;

import java.io.IOException;

public final class ElemDrop extends WasmU32VariantInstruction<ElementIndexData> {
    public static final ElemDrop INSTANCE = new ElemDrop();

    private ElemDrop() {
        super("elem.drop", (byte) 0xFC, 13);
    }

    @Override
    public ElementIndexData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return ElementIndexData.read(loader);
    }

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, ElementIndexData data) throws WasmAssemblerException {
        final ElementSegment elementSegment = context.getLookups().requireElementSegment(data.toArrayIndex());
        
        return new ProcessedInstruction() {
            @Override
            public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
                context.getGenerators().getTableGenerator().emitDropElement(
                        data.toArrayIndex(),
                        elementSegment,
                        context
                );
            }
            
            @Override
            public void processOutputs(CodeEmitContext context) {
            }
        };
    }
}
