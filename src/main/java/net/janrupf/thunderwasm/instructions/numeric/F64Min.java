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

public final class F64Min extends PlainNumeric {
    public static final F64Min INSTANCE = new F64Min();

    private F64Min() {
        super("f64.min", (byte) 0xA4);
    }

    @Override
    public void emitCode(WasmFrameState frameState, CodeEmitter emitter, EmptyInstructionData data) throws WasmAssemblerException {
        frameState.popOperand(NumberType.F64);
        frameState.requireOperand(NumberType.F64);
        emitter.invoke(
                ObjectType.of(Double.class),
                "min",
                new JavaType[]{PrimitiveType.DOUBLE, PrimitiveType.DOUBLE },
                PrimitiveType.DOUBLE,
                InvokeType.STATIC,
                false
        );
    }
}
