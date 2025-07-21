package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.imports.MemoryImportDescription;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.data.MemoryIndexData;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.MemoryType;
import net.janrupf.thunderwasm.types.NumberType;

import java.io.IOException;

public final class MemorySize extends WasmInstruction<MemoryIndexData> {
    public static final MemorySize INSTANCE = new MemorySize();

    public MemorySize() {
        super("memory.size", (byte) 0x3F);
    }

    @Override
    public MemoryIndexData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return MemoryIndexData.read(loader);
    }

    @Override
    public void emitCode(CodeEmitContext context, MemoryIndexData data) throws WasmAssemblerException {
        FoundElement<MemoryType, MemoryImportDescription> memory = context.getLookups().requireMemory(data.toArrayIndex());

        if (memory.isImport()) {
            context.getGenerators().getImportGenerator().emitMemorySize(
                    memory.getImport(),
                    context
            );
        } else {
            context.getGenerators().getMemoryGenerator().emitMemorySize(
                    memory.getIndex(),
                    memory.getElement(),
                    context
            );
        }

        context.getFrameState().pushOperand(NumberType.I32);
    }
}
