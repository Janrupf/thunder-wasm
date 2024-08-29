package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64Eqz extends PlainNumeric {
    public static final I64Eqz INSTANCE = new I64Eqz();

    private I64Eqz() {
        super("i64.eqz", (byte) 0x50);
    }

    @Override
    public void emitCode(
            WasmFrameState frameState,
            CodeEmitter emitter,
            EmptyInstructionData data
    ) throws WasmAssemblerException {
        frameState.pushOperand(NumberType.I64);
        emitter.loadConstant(0L);
        frameState.popOperand(NumberType.I64);
        frameState.popOperand(NumberType.I64);
        emitter.op(Op.LCMP);
        frameState.pushOperand(NumberType.I32);
        CommonBytecodeGenerator.evalCompResultZeroOrOne(frameState, emitter, ComparisonResult.EQUAL);
    }
}
