package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.ComparisonResult;
import net.janrupf.thunderwasm.assembler.emitter.CommonBytecodeGenerator;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;

public final class F64Gt extends PlainNumeric {
    public static final F64Gt INSTANCE = new F64Gt();

    private F64Gt() {
        super("f64.gt", (byte) 0x64);
    }

    @Override
    public void emitCode(
            CodeEmitContext context, EmptyInstructionData data
    ) throws WasmAssemblerException {
        CommonBytecodeGenerator.evalDoubleCompNaNZero(
                context.getFrameState(),
                context.getEmitter(),
                ComparisonResult.GREATER_THAN
        );
    }
}
