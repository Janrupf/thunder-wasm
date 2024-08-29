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

public final class I32Popcnt extends PlainNumeric {
    public static final I32Popcnt INSTANCE = new I32Popcnt();

    private I32Popcnt() {
        super("i32.popcnt", (byte) 0x69);
    }

    @Override
    public void emitCode(WasmFrameState frameState, CodeEmitter emitter, EmptyInstructionData data) throws WasmAssemblerException {
        frameState.requireOperand(NumberType.I32);
        emitter.invoke(
                ObjectType.of(Integer.class),
                "bitCount",
                new JavaType[]{PrimitiveType.INT},
                PrimitiveType.INT,
                InvokeType.STATIC,
                false
        );
    }
}
