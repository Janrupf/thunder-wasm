package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64Shl extends PlainNumeric {
    public static final I64Shl INSTANCE = new I64Shl();

    private I64Shl() {
        super("i64.shl", (byte) 0x86);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I64);
        context.getFrameState().requireOperand(NumberType.I64);
        context.getEmitter().op(Op.L2I);
        context.getEmitter().op(Op.LSHL);
    }
}
