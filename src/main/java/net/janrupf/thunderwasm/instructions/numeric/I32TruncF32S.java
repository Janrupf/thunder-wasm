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

public final class I32TruncF32S extends PlainNumeric {
    public static final I32TruncF32S INSTANCE = new I32TruncF32S();

    private I32TruncF32S() {
        super("i32.trunc_f32_s", (byte) 0xA8);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.F32);

        if (context.getConfiguration().strictNumericsEnabled()) {
            context.getEmitter().invoke(
                    ObjectType.of(WasmMath.class),
                    "strictI32TruncF32S",
                    new JavaType[]{PrimitiveType.FLOAT },
                    PrimitiveType.INT,
                    InvokeType.STATIC,
                    false
            );
        } else {
            context.getEmitter().op(Op.F2I);
        }

        context.getFrameState().pushOperand(NumberType.I32);
    }
}
