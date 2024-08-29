package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.InvokeType;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64DivU extends PlainNumeric {
    public static final I64DivU INSTANCE = new I64DivU();

    private I64DivU() {
        super("i64.div_u", (byte) 0x80);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I64);
        context.getFrameState().requireOperand(NumberType.I64);
        context.getEmitter().invoke(
                ObjectType.of(Long.class),
                "divideUnsigned",
                new JavaType[] {PrimitiveType.LONG, PrimitiveType.LONG },
                PrimitiveType.LONG,
                InvokeType.STATIC,
                false
        );
    }
}
