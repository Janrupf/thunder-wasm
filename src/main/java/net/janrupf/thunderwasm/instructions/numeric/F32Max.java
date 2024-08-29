package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.InvokeType;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class F32Max extends PlainNumeric {
    public static final F32Max INSTANCE = new F32Max();

    private F32Max() {
        super("f32.max", (byte) 0x97);
    }

    @Override
    public void emitCode(WasmFrameState frameState, CodeEmitter emitter, EmptyInstructionData data) throws WasmAssemblerException {
        frameState.popOperand(NumberType.F32);
        frameState.requireOperand(NumberType.F32);
        emitter.invoke(
                ObjectType.of(Float.class),
                "max",
                new JavaType[]{PrimitiveType.FLOAT, PrimitiveType.FLOAT },
                PrimitiveType.FLOAT,
                InvokeType.STATIC,
                false
        );
    }
}
