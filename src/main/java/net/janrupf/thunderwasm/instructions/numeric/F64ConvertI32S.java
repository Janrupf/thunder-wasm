package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class F64ConvertI32S extends PlainNumeric {
    public static final F64ConvertI32S INSTANCE = new F64ConvertI32S();

    private F64ConvertI32S() {
        super("f64.convert_i32_s", (byte) 0xB7);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().requireOperand(NumberType.I32);
        context.getEmitter().op(Op.I2D);
        context.getFrameState().pushOperand(NumberType.F64);
    }
}
