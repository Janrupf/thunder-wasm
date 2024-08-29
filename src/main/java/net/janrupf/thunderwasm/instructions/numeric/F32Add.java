package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class F32Add extends PlainNumeric {
    public static final F32Add INSTANCE = new F32Add();

    private F32Add() {
        super("f32.add", (byte) 0x92);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.F32);
        context.getFrameState().requireOperand(NumberType.F32);
        context.getEmitter().op(Op.FADD);
    }
}
