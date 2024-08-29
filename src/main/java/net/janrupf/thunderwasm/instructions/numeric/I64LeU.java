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

public final class I64LeU extends PlainNumeric {
    public static final I64LeU INSTANCE = new I64LeU();

    private I64LeU() {
        super("i64.le_u", (byte) 0x58);
    }

    @Override
    public void emitCode(
            WasmFrameState frameState,
            CodeEmitter emitter,
            EmptyInstructionData data
    ) throws WasmAssemblerException {
        CommonBytecodeGenerator.convertTop2Unsigned(frameState, emitter, NumberType.I64);

        frameState.popOperand(NumberType.I64);
        emitter.op(Op.LCMP);
        frameState.pushOperand(NumberType.I32);
        CommonBytecodeGenerator.evalCompResultZeroOrOne(frameState, emitter, ComparisonResult.LESS_THAN_OR_EQUAL);
    }
}
