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
import net.janrupf.thunderwasm.types.NumberType;

public final class F32Floor extends PlainNumeric {
    public static final F32Floor INSTANCE = new F32Floor();

    private F32Floor() {
        super("f32.floor", (byte) 0x8E);
    }

    @Override
    public void emitCode(WasmFrameState frameState, CodeEmitter emitter, EmptyInstructionData data) throws WasmAssemblerException {
        frameState.popOperand(NumberType.F32);
        emitter.op(Op.F2D);
        frameState.pushOperand(NumberType.F64);
        emitter.invoke(
                ObjectType.of(Math.class),
                "floor",
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
