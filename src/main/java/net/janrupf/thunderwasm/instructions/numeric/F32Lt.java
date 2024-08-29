package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;

public final class F32Lt extends PlainNumeric {
    public static final F32Lt INSTANCE = new F32Lt();

    private F32Lt() {
        super("f32.lt", (byte) 0x5D);
    }

    @Override
    public void emitCode(
            WasmFrameState frameState,
            CodeEmitter emitter,
            EmptyInstructionData data
    ) throws WasmAssemblerException {
        CommonBytecodeGenerator.evalFloatCompNaNZero(frameState, emitter, ComparisonResult.LESS_THAN);
    }
}
