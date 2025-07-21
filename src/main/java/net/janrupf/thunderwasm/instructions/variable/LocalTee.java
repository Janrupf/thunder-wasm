package net.janrupf.thunderwasm.instructions.variable;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
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
            CodeEmitContext context, LocalIndexData data
    ) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        ValueType type = frameState.requireLocal(data.getIndex());
        frameState.requireOperand(type);

        JavaType javaType = WasmTypeConverter.toJavaType(type);
        emitter.duplicate();
        frameState.pushOperand(type);
        emitter.storeLocal(emitter.getArgumentLocal(data.getIndex()));
        frameState.popOperand(type);
    }
}
