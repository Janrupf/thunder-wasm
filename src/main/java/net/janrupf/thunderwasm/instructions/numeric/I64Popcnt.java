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

public final class I64Popcnt extends PlainNumeric {
    public static final I64Popcnt INSTANCE = new I64Popcnt();

    private I64Popcnt() {
        super("i64.popcnt", (byte) 0x7B);
    }

    @Override
    public void emitCode(
            CodeEmitContext context, EmptyInstructionData data
    ) throws WasmAssemblerException {
        context.getFrameState().requireOperand(NumberType.I64);
        context.getEmitter().invoke(
                ObjectType.of(Long.class),
                "bitCount",
                new JavaType[]{PrimitiveType.LONG},
                PrimitiveType.INT,
                InvokeType.STATIC,
                false
        );
        context.getEmitter().op(Op.I2L);
    }
}
