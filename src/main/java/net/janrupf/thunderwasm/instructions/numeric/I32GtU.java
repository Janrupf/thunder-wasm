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

public final class I32GtU extends PlainNumeric {
    public static final I32GtU INSTANCE = new I32GtU();

    private I32GtU() {
        super("i32.gt_u", (byte) 0x4B);
    }

    @Override
    public void emitCode(
            CodeEmitContext context, EmptyInstructionData data
    ) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        CommonBytecodeGenerator.swapConvertUnsignedI32(emitter);

        // Operands are now ready for comparison, but backwards - instead of swapping them again,
        // we reverse the comparison condition
        frameState.popOperand(NumberType.I32);
        frameState.requireOperand(NumberType.I32);
        CommonBytecodeGenerator.evalConditionZeroOrOne(emitter, JumpCondition.INT_LESS_THAN);
    }
}
