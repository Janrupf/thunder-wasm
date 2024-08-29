package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.decoder.InstructionDecoder;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64TruncSatF64S extends PlainNumeric {
    public static final I64TruncSatF64S INSTANCE = new I64TruncSatF64S();

    private I64TruncSatF64S() {
        super("i64.trunc_sat_f64_s", (byte) 0xFC);
    }

    @Override
    public InstructionDecoder getDecoder() {
        return InstructionDecoder.u32Variant(6, this);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.F64);
        context.getEmitter().op(Op.D2L);
        context.getFrameState().pushOperand(NumberType.I64);
    }
}
