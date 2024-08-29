package net.janrupf.thunderwasm.instructions.variable;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.data.LocalIndexData;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.ValueType;

import java.io.IOException;

public final class LocalGet extends WasmInstruction<LocalIndexData> {
    public static final LocalGet INSTANCE = new LocalGet();

    private LocalGet() {
        super("local.get", (byte) 0x20);
    }

    @Override
    public LocalIndexData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return LocalIndexData.read(loader);
    }

    @Override
    public void emitCode(
            WasmFrameState frameState,
            CodeEmitter emitter,
            LocalIndexData data
    ) throws WasmAssemblerException {
        ValueType type = frameState.requireLocal(data.getIndex());
        emitter.loadLocal(frameState.computeJavaLocalIndex(data.getIndex()), WasmTypeConverter.toJavaType(type));
        frameState.pushOperand(type);
    }
}
