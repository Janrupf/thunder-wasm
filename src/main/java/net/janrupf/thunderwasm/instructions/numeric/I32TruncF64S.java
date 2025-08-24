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

public final class I32TruncF64S extends PlainNumeric {
    public static final I32TruncF64S INSTANCE = new I32TruncF64S();

    private I32TruncF64S() {
        super("i32.trunc_f64_s", (byte) 0xAA);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.F64);

        if (context.getConfiguration().strictNumericsEnabled()) {
            context.getEmitter().invoke(
                    ObjectType.of(WasmMath.class),
                    "strictI32TruncF64S",
                    new JavaType[]{PrimitiveType.DOUBLE},
                    PrimitiveType.INT,
                    InvokeType.STATIC,
                    false
            );
        } else {
            context.getEmitter().op(Op.D2I);
        }

        context.getFrameState().pushOperand(NumberType.I32);
    }
}
