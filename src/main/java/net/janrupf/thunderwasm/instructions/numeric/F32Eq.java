package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class F32Eq extends PlainNumeric {
    public static final F32Eq INSTANCE = new F32Eq();

    private F32Eq() {
        super("f32.eq", (byte) 0x5B);
    }

    @Override
    public void emitCode(
            WasmFrameState frameState,
            CodeEmitter emitter,
            EmptyInstructionData data
    ) throws WasmAssemblerException {
        frameState.popOperand(NumberType.F32);
        frameState.popOperand(NumberType.F32);
        emitter.op(Op.FCMPG);
        frameState.pushOperand(NumberType.I32);
        CommonBytecodeGenerator.evalCompResultZeroOrOne(frameState, emitter, ComparisonResult.EQUAL);
    }
}
