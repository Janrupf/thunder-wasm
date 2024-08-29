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

public final class F64Ceil extends PlainNumeric {
    public static final F64Ceil INSTANCE = new F64Ceil();

    private F64Ceil() {
        super("f64.ceil", (byte) 0x9B);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().requireOperand(NumberType.F64);
        context.getEmitter().invoke(
                ObjectType.of(Math.class),
                "ceil",
                new JavaType[]{PrimitiveType.DOUBLE},
                PrimitiveType.DOUBLE,
                InvokeType.STATIC,
                false
        );
    }
}
