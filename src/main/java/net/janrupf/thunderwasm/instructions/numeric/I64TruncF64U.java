package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64TruncF64U extends PlainNumeric {
    public static final I64TruncF64U INSTANCE = new I64TruncF64U();

    private I64TruncF64U() {
        super("i64.trunc_f64_u", (byte) 0xB1);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.F64);
        context.getEmitter().op(Op.D2L);
        context.getFrameState().pushOperand(NumberType.I64);
    }
}
