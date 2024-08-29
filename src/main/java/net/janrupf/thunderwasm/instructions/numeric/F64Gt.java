package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
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
            WasmFrameState frameState,
            CodeEmitter emitter,
            EmptyInstructionData data
    ) throws WasmAssemblerException {
        CommonBytecodeGenerator.evalDoubleCompNaNZero(frameState, emitter, ComparisonResult.GREATER_THAN);
    }
}
