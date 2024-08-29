package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32Sub extends PlainNumeric {
    public static final I32Sub INSTANCE = new I32Sub();

    private I32Sub() {
        super("i32.sub", (byte) 0x6B);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I32);
        context.getFrameState().requireOperand(NumberType.I32);
        context.getEmitter().op(Op.ISUB);
    }
}
