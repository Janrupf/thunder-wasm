package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.InvokeType;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.runtime.WasmMath;
import net.janrupf.thunderwasm.types.NumberType;

public final class F32Trunc extends PlainNumeric {
    public static final F32Trunc INSTANCE = new F32Trunc();

    private F32Trunc() {
        super("f32.trunc", (byte) 0x8F);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().requireOperand(NumberType.F32);
        context.getEmitter().invoke(
                ObjectType.of(WasmMath.class),
                "floatTrunc",
                new JavaType[]{PrimitiveType.FLOAT},
                PrimitiveType.FLOAT,
                InvokeType.STATIC,
                false
        );
    }
}
