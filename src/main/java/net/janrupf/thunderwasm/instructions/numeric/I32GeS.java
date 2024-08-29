package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.CommonBytecodeGenerator;
import net.janrupf.thunderwasm.assembler.emitter.JumpCondition;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32GeS extends PlainNumeric {
    public static final I32GeS INSTANCE = new I32GeS();

    private I32GeS() {
        super("i32.ge_s", (byte) 0x4E);
    }

    @Override
    public void emitCode(
            WasmFrameState frameState,
            CodeEmitter emitter,
            EmptyInstructionData data
    ) throws WasmAssemblerException {
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(NumberType.I32);
        CommonBytecodeGenerator.evalConditionZeroOrOne(frameState, emitter, JumpCondition.INT_GREATER_THAN_OR_EQUAL);
    }
}
