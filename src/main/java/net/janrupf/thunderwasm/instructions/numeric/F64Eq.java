package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.ComparisonResult;
import net.janrupf.thunderwasm.assembler.emitter.CommonBytecodeGenerator;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class F64Eq extends PlainNumeric {
    public static final F64Eq INSTANCE = new F64Eq();

    private F64Eq() {
        super("f64.eq", (byte) 0x61);
    }

    @Override
    public void emitCode(
            WasmFrameState frameState,
            CodeEmitter emitter,
            EmptyInstructionData data
    ) throws WasmAssemblerException {
        frameState.popOperand(NumberType.F64);
        frameState.popOperand(NumberType.F64);
        emitter.op(Op.DCMPG);
        frameState.pushOperand(NumberType.I32);
        CommonBytecodeGenerator.evalCompResultZeroOrOne(frameState, emitter, ComparisonResult.EQUAL);
    }
}
