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
import net.janrupf.thunderwasm.types.NumberType;

public final class I32ReinterpretF32 extends PlainNumeric {
    public static final I32ReinterpretF32 INSTANCE = new I32ReinterpretF32();

    private I32ReinterpretF32() {
        super("i32.reinterpret_f32", (byte) 0xBC);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.F32);
        context.getEmitter().invoke(
                ObjectType.of(Float.class),
                "floatToRawIntBits",
                new JavaType[]{ PrimitiveType.FLOAT },
                PrimitiveType.INT,
                InvokeType.STATIC,
                false
        );
        context.getFrameState().pushOperand(NumberType.I32);
    }
}
