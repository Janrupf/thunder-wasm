package net.janrupf.thunderwasm.instructions.variable;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.data.LocalIndexData;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.ValueType;

import java.io.IOException;

public final class LocalTee extends WasmInstruction<LocalIndexData> {
    public static final LocalTee INSTANCE = new LocalTee();

    private LocalTee() {
        super("local.tee", (byte) 0x22);
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
        frameState.requireOperand(type);

        JavaType javaType = WasmTypeConverter.toJavaType(type);
        emitter.duplicate(javaType);
        frameState.pushOperand(type);
        emitter.storeLocal(frameState.computeJavaLocalIndex(data.getIndex()), javaType);
        frameState.popOperand(type);
    }
}
