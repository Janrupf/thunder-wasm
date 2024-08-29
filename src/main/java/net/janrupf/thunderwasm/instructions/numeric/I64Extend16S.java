package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64Extend16S extends PlainNumeric {
    public static final I64Extend16S INSTANCE = new I64Extend16S();

    private I64Extend16S() {
        super("i64.extend16_s", (byte) 0xC3);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().requireOperand(NumberType.I64);
        context.getEmitter().op(Op.L2I);
        context.getEmitter().op(Op.I2S);
        context.getEmitter().op(Op.I2L);
    }
}
