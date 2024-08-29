package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64Extend8S extends PlainNumeric {
    public static final I64Extend8S INSTANCE = new I64Extend8S();

    private I64Extend8S() {
        super("i64.extend8_s", (byte) 0xC2);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().requireOperand(NumberType.I64);
        context.getEmitter().op(Op.L2I);
        context.getEmitter().op(Op.I2B);
        context.getEmitter().op(Op.I2L);
    }
}
