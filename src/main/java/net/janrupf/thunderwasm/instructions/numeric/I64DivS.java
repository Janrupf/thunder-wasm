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

public final class I64DivS extends PlainNumeric {
    public static final I64DivS INSTANCE = new I64DivS();

    private I64DivS() {
        super("i64.div_s", (byte) 0x7F);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I64);
        context.getFrameState().requireOperand(NumberType.I64);

        if (context.getConfiguration().strictNumericsEnabled()) {
            context.getEmitter().invoke(
                    ObjectType.of(Math.class),
                    "divideExact",
                    new JavaType[]{PrimitiveType.LONG, PrimitiveType.LONG},
                    PrimitiveType.LONG,
                    InvokeType.STATIC,
                    false
            );
        } else {
            context.getEmitter().op(Op.LDIV);
        }
    }
}
