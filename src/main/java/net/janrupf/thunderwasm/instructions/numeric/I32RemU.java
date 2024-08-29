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

public final class I32RemU extends PlainNumeric {
    public static final I32RemU INSTANCE = new I32RemU();

    private I32RemU() {
        super("i32.rem_u", (byte) 0x70);
    }

    @Override
    public void emitCode(WasmFrameState frameState, CodeEmitter emitter, EmptyInstructionData data) throws WasmAssemblerException {
        frameState.popOperand(NumberType.I32);
        frameState.requireOperand(NumberType.I32);
        emitter.invoke(
                ObjectType.of(Integer.class),
                "remainderUnsigned",
                new JavaType[]{PrimitiveType.INT, PrimitiveType.INT},
                PrimitiveType.INT,
                InvokeType.STATIC,
                false
        );
    }
}
