package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.decoder.InstructionDecoder;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32TruncSatF32S extends PlainNumeric {
    public static final I32TruncSatF32S INSTANCE = new I32TruncSatF32S();

    private I32TruncSatF32S() {
        super("i32.trunc_sat_f32_s", (byte) 0xFC);
    }

    @Override
    public InstructionDecoder getDecoder() {
        return InstructionDecoder.u32Variant(0, this);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.F32);
        context.getEmitter().op(Op.F2I);
        context.getFrameState().pushOperand(NumberType.I32);
    }
}
