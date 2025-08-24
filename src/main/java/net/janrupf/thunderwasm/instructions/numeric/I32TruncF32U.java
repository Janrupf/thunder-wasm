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

public final class I32TruncF32U extends PlainNumeric {
    public static final I32TruncF32U INSTANCE = new I32TruncF32U();

    private I32TruncF32U() {
        super("i32.trunc_f32_u", (byte) 0xA9);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        frameState.popOperand(NumberType.F32);

        if (context.getConfiguration().strictNumericsEnabled()) {
            emitter.invoke(
                    ObjectType.of(WasmMath.class),
                    "strictI32TruncF32U",
                    new JavaType[]{PrimitiveType.FLOAT},
                    PrimitiveType.INT,
                    InvokeType.STATIC,
                    false
            );
        } else {
            emitter.op(Op.F2L);
            emitter.op(Op.L2I);
        }

        frameState.pushOperand(NumberType.I32);
    }
}
