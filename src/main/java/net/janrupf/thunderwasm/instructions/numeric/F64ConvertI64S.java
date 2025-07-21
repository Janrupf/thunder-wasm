package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class F64ConvertI64S extends PlainNumeric {
    public static final F64ConvertI64S INSTANCE = new F64ConvertI64S();

    private F64ConvertI64S() {
        super("f64.convert_i64_s", (byte) 0xB9);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I64);
        context.getEmitter().op(Op.L2D);
        context.getFrameState().pushOperand(NumberType.F64);
    }
}
