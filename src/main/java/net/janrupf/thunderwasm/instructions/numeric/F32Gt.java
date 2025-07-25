package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class F32Gt extends PlainNumeric {
    public static final F32Gt INSTANCE = new F32Gt();

    private F32Gt() {
        super("f32.gt", (byte) 0x5E);
    }

    @Override
    public void emitCode(
            CodeEmitContext context, EmptyInstructionData data
    ) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.F32);
        context.getFrameState().popOperand(NumberType.F32);

        CommonBytecodeGenerator.evalFloatCompNaNZero(
                context.getEmitter(),
                ComparisonResult.GREATER_THAN
        );

        context.getFrameState().pushOperand(NumberType.I32);
    }
}
