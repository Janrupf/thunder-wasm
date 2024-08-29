package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32WrapI64 extends PlainNumeric {
    public static final I32WrapI64 INSTANCE = new I32WrapI64();

    private I32WrapI64() {
        super("i32.wrap_i64", (byte) 0xA7);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I64);
        context.getEmitter().op(Op.L2I);
        context.getFrameState().pushOperand(NumberType.I32);
    }
}
