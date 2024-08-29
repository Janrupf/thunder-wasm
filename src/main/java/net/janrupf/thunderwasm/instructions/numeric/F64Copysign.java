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

public final class F64Copysign extends PlainNumeric {
    public static final F64Copysign INSTANCE = new F64Copysign();

    private F64Copysign() {
        super("f64.copysign", (byte) 0xA6);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.F64);
        context.getFrameState().requireOperand(NumberType.F64);
        context.getEmitter().invoke(
                ObjectType.of(Math.class),
                "copySign",
                new JavaType[]{PrimitiveType.DOUBLE, PrimitiveType.DOUBLE },
                PrimitiveType.DOUBLE,
                InvokeType.STATIC,
                false
        );
    }
}
