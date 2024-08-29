package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.InvokeType;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.runtime.WasmMath;
import net.janrupf.thunderwasm.types.NumberType;

public final class F32Sqrt extends PlainNumeric {
    public static final F32Sqrt INSTANCE = new F32Sqrt();

    private F32Sqrt() {
        super("f32.sqrt", (byte) 0x91);
    }

    @Override
    public void emitCode(WasmFrameState frameState, CodeEmitter emitter, EmptyInstructionData data) throws WasmAssemblerException {
        frameState.popOperand(NumberType.F32);
        emitter.op(Op.F2D);
        frameState.pushOperand(NumberType.F64);
        emitter.invoke(
                ObjectType.of(Math.class),
                "sqrt",
                new JavaType[]{PrimitiveType.DOUBLE},
                PrimitiveType.DOUBLE,
                InvokeType.STATIC,
                false
        );
        frameState.popOperand(NumberType.F64);
        emitter.op(Op.D2F);
        frameState.pushOperand(NumberType.F32);
    }
}
