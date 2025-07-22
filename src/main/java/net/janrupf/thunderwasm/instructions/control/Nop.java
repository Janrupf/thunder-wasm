package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.WasmLoader;

public final class Nop extends WasmInstruction<EmptyInstructionData> {
    public static final Nop INSTANCE = new Nop();

    private Nop() {
        super("nop", (byte) 0x01);
    }

    @Override
    public EmptyInstructionData readData(WasmLoader loader) {
        return EmptyInstructionData.INSTANCE;
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        // No-op
    }
}
