package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class F64PromoteF32 extends PlainNumeric {
    public static final F64PromoteF32 INSTANCE = new F64PromoteF32();

    private F64PromoteF32() {
        super("f64.promote_f32", (byte) 0xBB);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().requireOperand(NumberType.F32);
        context.getEmitter().op(Op.F2D);
        context.getFrameState().pushOperand(NumberType.F64);
    }
}
