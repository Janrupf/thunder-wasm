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

public final class F64Max extends PlainNumeric {
    public static final F64Max INSTANCE = new F64Max();

    private F64Max() {
        super("f64.max", (byte) 0xA5);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.F64);
        context.getFrameState().requireOperand(NumberType.F64);
        context.getEmitter().invoke(
                ObjectType.of(Double.class),
                "max",
                new JavaType[]{PrimitiveType.DOUBLE, PrimitiveType.DOUBLE },
                PrimitiveType.DOUBLE,
                InvokeType.STATIC,
                false
        );
    }
}
