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

public final class I64RemU extends PlainNumeric {
    public static final I64RemU INSTANCE = new I64RemU();

    private I64RemU() {
        super("i64.rem_u", (byte) 0x82);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I64);
        context.getFrameState().requireOperand(NumberType.I64);
        context.getEmitter().invoke(
                ObjectType.of(Long.class),
                "remainderUnsigned",
                new JavaType[] {PrimitiveType.LONG, PrimitiveType.LONG },
                PrimitiveType.LONG,
                InvokeType.STATIC,
                false
        );
    }
}
