package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64Clz extends PlainNumeric {
    public static final I64Clz INSTANCE = new I64Clz();

    private I64Clz() {
        super("i64.clz", (byte) 0x79);
    }

    @Override
    public void emitCode(
            CodeEmitContext context, EmptyInstructionData data
    ) throws WasmAssemblerException {
        context.getFrameState().requireOperand(NumberType.I64);
        context.getEmitter().invoke(
                ObjectType.of(Long.class),
                "numberOfLeadingZeros",
                new JavaType[]{PrimitiveType.LONG},
                PrimitiveType.INT,
                InvokeType.STATIC,
                false
        );
        context.getEmitter().op(Op.I2L);
    }
}
