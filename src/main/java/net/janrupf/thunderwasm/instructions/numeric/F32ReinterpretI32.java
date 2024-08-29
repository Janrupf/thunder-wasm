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

public final class F32ReinterpretI32 extends PlainNumeric {
    public static final F32ReinterpretI32 INSTANCE = new F32ReinterpretI32();

    private F32ReinterpretI32() {
        super("f32.reinterpret_i32", (byte) 0xBE);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I32);
        context.getEmitter().invoke(
                ObjectType.of(Float.class),
                "intBitsToFloat",
                new JavaType[]{ PrimitiveType.INT },
                PrimitiveType.FLOAT,
                InvokeType.STATIC,
                false
        );
        context.getFrameState().pushOperand(NumberType.F32);
    }
}
