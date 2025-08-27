package net.janrupf.thunderwasm.instructions.parametric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class Drop extends WasmInstruction<EmptyInstructionData> implements ProcessedInstruction {
    public static final Drop INSTANCE = new Drop();

    private Drop() {
        super("drop", (byte) 0x1A);
    }

    @Override
    public EmptyInstructionData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return EmptyInstructionData.INSTANCE;
    }

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popAnyOperand();
        return this;
    }

    @Override
    public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
        context.getEmitter().pop();
    }

    @Override
    public void processOutputs(CodeEmitContext context) throws WasmAssemblerException {
    }
}
