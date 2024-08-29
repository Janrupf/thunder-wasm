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

public final class I64ReinterpretF64 extends PlainNumeric {
    public static final I64ReinterpretF64 INSTANCE = new I64ReinterpretF64();

    private I64ReinterpretF64() {
        super("i64.reinterpret_f64", (byte) 0xBD);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.F64);
        context.getEmitter().invoke(
                ObjectType.of(Double.class),
                "doubleToRawLongBits",
                new JavaType[]{ PrimitiveType.DOUBLE },
                PrimitiveType.LONG,
                InvokeType.STATIC,
                false
        );
        context.getFrameState().pushOperand(NumberType.I64);
    }
}
