package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.CommonBytecodeGenerator;
import net.janrupf.thunderwasm.assembler.emitter.JumpCondition;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32GeU extends PlainNumeric {
    public static final I32GeU INSTANCE = new I32GeU();

    private I32GeU() {
        super("i32.ge_u", (byte) 0x4F);
    }

    @Override
    public void emitCode(
            CodeEmitContext context, EmptyInstructionData data
    ) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        CommonBytecodeGenerator.swapConvertUnsignedI32(frameState, emitter);

        // Operands are now ready for comparison, but backwards - instead of swapping them again,
        // we reverse the comparison condition
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(NumberType.I32);
        CommonBytecodeGenerator.evalConditionZeroOrOne(frameState, emitter, JumpCondition.INT_LESS_THAN_OR_EQUAL);
    }
}
