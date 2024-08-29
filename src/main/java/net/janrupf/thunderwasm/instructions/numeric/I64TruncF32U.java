package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64TruncF32U extends PlainNumeric {
    public static final I64TruncF32U INSTANCE = new I64TruncF32U();

    private I64TruncF32U() {
        super("i64.trunc_f32_u", (byte) 0xAF);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.F32);
        context.getEmitter().op(Op.F2L);
        context.getFrameState().pushOperand(NumberType.I64);
    }
}
