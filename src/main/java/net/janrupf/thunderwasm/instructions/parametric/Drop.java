package net.janrupf.thunderwasm.instructions.parametric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.ValueType;

import java.io.IOException;

public final class Drop extends WasmInstruction<EmptyInstructionData> {
    public static final Drop INSTANCE = new Drop();

    private Drop() {
        super("drop", (byte) 0x1A);
    }

    @Override
    public EmptyInstructionData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return EmptyInstructionData.INSTANCE;
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        ValueType dropped = context.getFrameState().popAnyOperand();
        context.getEmitter().pop();
    }
}
