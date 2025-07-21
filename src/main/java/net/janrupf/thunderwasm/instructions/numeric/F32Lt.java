package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class F32Lt extends PlainNumeric {
    public static final F32Lt INSTANCE = new F32Lt();

    private F32Lt() {
        super("f32.lt", (byte) 0x5D);
    }

    @Override
    public void emitCode(
            CodeEmitContext context, EmptyInstructionData data
    ) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.F32);
        context.getFrameState().popOperand(NumberType.F32);

        CommonBytecodeGenerator.evalFloatCompNaNZero(
                context.getEmitter(),
                ComparisonResult.LESS_THAN
        );

        context.getFrameState().popOperand(NumberType.I32);
    }
}
