package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.ComparisonResult;
import net.janrupf.thunderwasm.assembler.emitter.CommonBytecodeGenerator;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;

public final class F64Lt extends PlainNumeric {
    public static final F64Lt INSTANCE = new F64Lt();

    private F64Lt() {
        super("f64.lt", (byte) 0x63);
    }

    @Override
    public void emitCode(
            CodeEmitContext context, EmptyInstructionData data
    ) throws WasmAssemblerException {
        CommonBytecodeGenerator.evalDoubleCompNaNZero(
                context.getFrameState(),
                context.getEmitter(),
                ComparisonResult.LESS_THAN
        );
    }
}
