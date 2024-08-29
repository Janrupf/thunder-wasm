package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;

public final class F32Gt extends PlainNumeric {
    public static final F32Gt INSTANCE = new F32Gt();

    private F32Gt() {
        super("f32.gt", (byte) 0x5E);
    }

    @Override
    public void emitCode(
            WasmFrameState frameState,
            CodeEmitter emitter,
            EmptyInstructionData data
    ) throws WasmAssemblerException {
        CommonBytecodeGenerator.evalFloatCompNaNZero(frameState, emitter, ComparisonResult.GREATER_THAN);
    }
}
