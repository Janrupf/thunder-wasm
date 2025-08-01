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
import net.janrupf.thunderwasm.types.NumberType;

public final class F32Ceil extends PlainNumeric {
    public static final F32Ceil INSTANCE = new F32Ceil();

    private F32Ceil() {
        super("f32.ceil", (byte) 0x8D);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        frameState.requireOperand(NumberType.F32);

        emitter.op(Op.F2D);

        emitter.invoke(
                ObjectType.of(Math.class),
                "ceil",
                new JavaType[]{PrimitiveType.DOUBLE},
                PrimitiveType.DOUBLE,
                InvokeType.STATIC,
                false
        );

        emitter.op(Op.D2F);
    }
}
