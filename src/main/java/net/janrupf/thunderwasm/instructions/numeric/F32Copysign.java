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

public final class F32Copysign extends PlainNumeric {
    public static final F32Copysign INSTANCE = new F32Copysign();

    private F32Copysign() {
        super("f32.copysign", (byte) 0x98);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.F32);
        context.getFrameState().requireOperand(NumberType.F32);
        context.getEmitter().invoke(
                ObjectType.of(Math.class),
                "copySign",
                new JavaType[]{PrimitiveType.FLOAT, PrimitiveType.FLOAT },
                PrimitiveType.FLOAT,
                InvokeType.STATIC,
                false
        );
    }
}
