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

public final class F64ReinterpretI64 extends PlainNumeric {
    public static final F64ReinterpretI64 INSTANCE = new F64ReinterpretI64();

    private F64ReinterpretI64() {
        super("f64.reinterpret_i64", (byte) 0xBF);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I64);
        context.getEmitter().invoke(
                ObjectType.of(Double.class),
                "longBitsToDouble",
                new JavaType[]{ PrimitiveType.LONG },
                PrimitiveType.DOUBLE,
                InvokeType.STATIC,
                false
        );
        context.getFrameState().pushOperand(NumberType.F64);
    }
}
