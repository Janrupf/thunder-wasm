package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64Add extends PlainNumeric {
    public static final I64Add INSTANCE = new I64Add();

    private I64Add() {
        super("i64.add", (byte) 0x7C);
    }

    @Override
    public void emitCode(WasmFrameState frameState, CodeEmitter emitter, EmptyInstructionData data) throws WasmAssemblerException {
        frameState.popOperand(NumberType.I64);
        frameState.requireOperand(NumberType.I64);
        emitter.op(Op.LADD);
    }
}
