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

public final class F64ConvertI64U extends PlainNumeric {
    public static final F64ConvertI64U INSTANCE = new F64ConvertI64U();

    private F64ConvertI64U() {
        super("f64.convert_i64_u", (byte) 0xBA);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I64);
        context.getEmitter().invoke(
                ObjectType.of(WasmMath.class),
                "u64ToF64",
                new JavaType[]{PrimitiveType.LONG },
                PrimitiveType.DOUBLE,
                InvokeType.STATIC,
                false
        );
        context.getFrameState().pushOperand(NumberType.F64);
    }
}
