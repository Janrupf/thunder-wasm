package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.ComparisonResult;
import net.janrupf.thunderwasm.assembler.emitter.CommonBytecodeGenerator;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class F64Ge extends PlainNumeric {
    public static final F64Ge INSTANCE = new F64Ge();

    private F64Ge() {
        super("f64.ge", (byte) 0x66);
    }

    @Override
    public void emitCode(
            CodeEmitContext context, EmptyInstructionData data
    ) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.F64);
        context.getFrameState().popOperand(NumberType.F64);

        CommonBytecodeGenerator.evalDoubleCompNaNZero(
                context.getEmitter(),
                ComparisonResult.GREATER_THAN_OR_EQUAL
        );

        context.getFrameState().pushOperand(NumberType.I32);
    }
}
