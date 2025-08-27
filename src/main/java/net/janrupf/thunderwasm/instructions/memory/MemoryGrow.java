package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.imports.MemoryImportDescription;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.data.MemoryIndexData;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.MemoryType;
import net.janrupf.thunderwasm.types.NumberType;

import java.io.IOException;

public final class MemoryGrow extends WasmInstruction<MemoryIndexData> {
    public static final MemoryGrow INSTANCE = new MemoryGrow();

    public MemoryGrow() {
        super("memory.grow", (byte) 0x40);
    }

    @Override
    public MemoryIndexData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return MemoryIndexData.read(loader);
    }

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, MemoryIndexData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I32);
        
        final FoundElement<MemoryType, MemoryImportDescription> memory = context.getLookups().requireMemory(data.toArrayIndex());
        
        return new ProcessedInstruction() {
            @Override
            public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
                if (memory.isImport()) {
                    context.getGenerators().getImportGenerator().emitMemoryGrow(
                            memory.getImport(),
                            context
                    );
                } else {
                    context.getGenerators().getMemoryGenerator().emitMemoryGrow(
                            memory.getIndex(),
                            memory.getElement(),
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
