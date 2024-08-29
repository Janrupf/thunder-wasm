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

public final class F32Nearest extends PlainNumeric {
    public static final F32Nearest INSTANCE = new F32Nearest();

    private F32Nearest() {
        super("f32.nearest", (byte) 0x90);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().requireOperand(NumberType.F32);
        context.getEmitter().invoke(
                ObjectType.of(WasmMath.class),
                "floatNearest",
                new JavaType[]{PrimitiveType.FLOAT},
                PrimitiveType.FLOAT,
                InvokeType.STATIC,
                false
        );
    }
}
