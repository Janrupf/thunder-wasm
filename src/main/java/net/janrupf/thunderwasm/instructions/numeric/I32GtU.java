package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CommonBytecodeGenerator;
import net.janrupf.thunderwasm.assembler.emitter.JumpCondition;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32GtU extends PlainNumeric implements ProcessedInstruction {
    public static final I32GtU INSTANCE = new I32GtU();

    private I32GtU() {
        super("i32.gt_u", (byte) 0x4B);
    }

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I32);
        context.getFrameState().popOperand(NumberType.I32);
        return this;
    }

    @Override
    public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
        CommonBytecodeGenerator.swapConvertUnsignedI32(context.getEmitter());

        // Operands are now ready for comparison, but backwards - instead of swapping them again,
        // we reverse the comparison condition
        CommonBytecodeGenerator.evalConditionZeroOrOne(
                context.getEmitter(),
                JumpCondition.INT_LESS_THAN
        );
    }

    @Override
    public void processOutputs(CodeEmitContext context) throws WasmAssemblerException {
        context.getFrameState().pushOperand(NumberType.I32);
    }
}
