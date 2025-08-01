package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32TruncF32U extends PlainNumeric {
    public static final I32TruncF32U INSTANCE = new I32TruncF32U();

    private I32TruncF32U() {
        super("i32.trunc_f32_u", (byte) 0xA9);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        frameState.popOperand(NumberType.F32);
        emitter.op(Op.F2L);
        emitter.op(Op.L2I);
        frameState.pushOperand(NumberType.I32);
    }
}
