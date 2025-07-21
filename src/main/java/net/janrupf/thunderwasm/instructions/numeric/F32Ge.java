package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.ComparisonResult;
import net.janrupf.thunderwasm.assembler.emitter.CommonBytecodeGenerator;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class F32Ge extends PlainNumeric {
    public static final F32Ge INSTANCE = new F32Ge();

    private F32Ge() {
        super("f32.ge", (byte) 0x60);
    }

    @Override
    public void emitCode(
            CodeEmitContext context, EmptyInstructionData data
    ) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.F32);
        context.getFrameState().popOperand(NumberType.F32);

        CommonBytecodeGenerator.evalFloatCompNaNZero(
                context.getEmitter(),
                ComparisonResult.GREATER_THAN_OR_EQUAL
        );

        context.getFrameState().pushOperand(NumberType.I32);
    }
}
