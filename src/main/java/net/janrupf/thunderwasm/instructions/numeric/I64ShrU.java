package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64ShrU extends PlainNumeric {
    public static final I64ShrU INSTANCE = new I64ShrU();

    private I64ShrU() {
        super("i64.shr_u", (byte) 0x88);
    }

    @Override
    public void emitCode(WasmFrameState frameState, CodeEmitter emitter, EmptyInstructionData data) throws WasmAssemblerException {
        frameState.popOperand(NumberType.I64);
        frameState.requireOperand(NumberType.I64);
        emitter.op(Op.L2I);
        emitter.op(Op.LUSHR);
    }
}
