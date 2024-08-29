package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.InvokeType;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class F32Abs extends PlainNumeric {
    public static final F32Abs INSTANCE = new F32Abs();

    private F32Abs() {
        super("f32.abs", (byte) 0x8B);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().requireOperand(NumberType.F32);
        context.getEmitter().invoke(
                ObjectType.of(Math.class),
                "abs",
                new JavaType[]{PrimitiveType.FLOAT},
                PrimitiveType.FLOAT,
                InvokeType.STATIC,
                false
        );
    }
}
