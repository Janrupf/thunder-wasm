package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.ComparisonResult;
import net.janrupf.thunderwasm.assembler.emitter.CommonBytecodeGenerator;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class F64Le extends PlainNumeric {
    public static final F64Le INSTANCE = new F64Le();

    private F64Le() {
        super("f64.le", (byte) 0x65);
    }

    @Override
    public void emitCode(
            CodeEmitContext context, EmptyInstructionData data
    ) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.F64);
        context.getFrameState().popOperand(NumberType.F64);

        CommonBytecodeGenerator.evalDoubleCompNaNZero(
                context.getEmitter(),
                ComparisonResult.LESS_THAN_OR_EQUAL
        );

        context.getFrameState().pushOperand(NumberType.I32);
    }
}
