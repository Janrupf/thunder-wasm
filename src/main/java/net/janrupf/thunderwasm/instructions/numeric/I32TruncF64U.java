package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.InvokeType;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.runtime.WasmMath;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32TruncF64U extends PlainNumeric {
    public static final I32TruncF64U INSTANCE = new I32TruncF64U();

    private I32TruncF64U() {
        super("i32.trunc_f64_u", (byte) 0xAB);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        frameState.popOperand(NumberType.F64);

        if (context.getConfiguration().strictNumericsEnabled()) {
            emitter.invoke(
                    ObjectType.of(WasmMath.class),
                    "strictI32TruncF64U",
                    new JavaType[]{PrimitiveType.DOUBLE},
                    PrimitiveType.INT,
                    InvokeType.STATIC,
                    false
            );
        } else {
            emitter.op(Op.D2L);
            emitter.op(Op.L2I);
        }

        frameState.pushOperand(NumberType.I32);
    }
}
