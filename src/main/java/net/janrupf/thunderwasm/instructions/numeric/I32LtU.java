package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CommonBytecodeGenerator;
import net.janrupf.thunderwasm.assembler.emitter.JumpCondition;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32LtU extends PlainNumeric implements ProcessedInstruction {
    public static final I32LtU INSTANCE = new I32LtU();

    private I32LtU() {
        super("i32.lt_u", (byte) 0x49);
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
                JumpCondition.INT_GREATER_THAN
        );
    }

    @Override
    public void processOutputs(CodeEmitContext context) throws WasmAssemblerException {
        // Push I32 result onto stack (0 or 1)
        context.getFrameState().pushOperand(NumberType.I32);
    }
}
