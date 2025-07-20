package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.instructions.WasmU32VariantInstruction;
import net.janrupf.thunderwasm.instructions.data.DoubleIndexData;
import net.janrupf.thunderwasm.instructions.data.MemoryIndexData;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class MemoryCopy extends WasmU32VariantInstruction<DoubleIndexData<MemoryIndexData, MemoryIndexData>> {
    public static final MemoryCopy INSTANCE = new MemoryCopy();

    public MemoryCopy() {
        super("memory.copy", (byte) 0xFC, 10);
    }

    @Override
    public DoubleIndexData<MemoryIndexData, MemoryIndexData> readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return new DoubleIndexData<>(
            MemoryIndexData.read(loader),
            MemoryIndexData.read(loader)
        );
    }

    @Override
    public void emitCode(CodeEmitContext context, DoubleIndexData<MemoryIndexData, MemoryIndexData> data) throws WasmAssemblerException {
    }
}
