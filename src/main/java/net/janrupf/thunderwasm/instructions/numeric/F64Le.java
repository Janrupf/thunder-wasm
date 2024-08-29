package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.ComparisonResult;
import net.janrupf.thunderwasm.assembler.emitter.CommonBytecodeGenerator;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;

public final class F64Le extends PlainNumeric {
    public static final F64Le INSTANCE = new F64Le();

    private F64Le() {
        super("f64.le", (byte) 0x65);
    }

    @Override
    public void emitCode(
            CodeEmitContext context, EmptyInstructionData data
    ) throws WasmAssemblerException {
        CommonBytecodeGenerator.evalDoubleCompNaNZero(
                context.getFrameState(),
                context.getEmitter(),
                ComparisonResult.LESS_THAN_OR_EQUAL
        );
    }
}
