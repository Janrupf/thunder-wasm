package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64Extend32S extends PlainNumeric {
    public static final I64Extend32S INSTANCE = new I64Extend32S();

    private I64Extend32S() {
        super("i64.extend32_s", (byte) 0xC4);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().requireOperand(NumberType.I64);
        context.getEmitter().op(Op.L2I);
        context.getEmitter().op(Op.I2L);
    }
}
