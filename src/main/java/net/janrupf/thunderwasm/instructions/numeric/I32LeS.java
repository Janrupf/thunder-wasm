package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.CommonBytecodeGenerator;
import net.janrupf.thunderwasm.assembler.emitter.JumpCondition;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32LeS extends PlainNumeric {
    public static final I32LeS INSTANCE = new I32LeS();

    private I32LeS() {
        super("i32.le_s", (byte) 0x4C);
    }

    @Override
    public void emitCode(
            WasmFrameState frameState,
            CodeEmitter emitter,
            EmptyInstructionData data
    ) throws WasmAssemblerException {
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(NumberType.I32);
        CommonBytecodeGenerator.evalConditionZeroOrOne(frameState, emitter, JumpCondition.INT_LESS_THAN_OR_EQUAL);
    }
}
