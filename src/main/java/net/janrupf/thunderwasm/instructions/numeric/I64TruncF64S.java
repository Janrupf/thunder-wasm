package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.InvokeType;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.runtime.WasmMath;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64TruncF64S extends PlainNumeric {
    public static final I64TruncF64S INSTANCE = new I64TruncF64S();

    private I64TruncF64S() {
        super("i64.trunc_f64_s", (byte) 0xB0);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.F64);

        if (context.getConfiguration().strictNumericsEnabled()) {
            context.getEmitter().invoke(
                    ObjectType.of(WasmMath.class),
                    "strictI64TruncF64S",
                    new JavaType[]{PrimitiveType.DOUBLE},
                    PrimitiveType.LONG,
                    InvokeType.STATIC,
                    false
            );
        } else {
            context.getEmitter().op(Op.D2L);
        }

        context.getFrameState().pushOperand(NumberType.I64);
    }
}
