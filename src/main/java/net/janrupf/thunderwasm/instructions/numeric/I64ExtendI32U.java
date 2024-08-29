package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64ExtendI32U extends PlainNumeric {
    public static final I64ExtendI32U INSTANCE = new I64ExtendI32U();

    private I64ExtendI32U() {
        super("i64.extend_i32_u", (byte) 0xAD);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I32);
        context.getEmitter().op(Op.I2L);
        context.getFrameState().pushOperand(NumberType.I64);
        context.getFrameState().pushOperand(NumberType.I64);
        context.getEmitter().loadConstant(0x00000000FFFFFFFFL);
        context.getEmitter().op(Op.LAND);
        context.getFrameState().popOperand(NumberType.I64);
    }
}
