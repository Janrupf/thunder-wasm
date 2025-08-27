package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CommonBytecodeGenerator;
import net.janrupf.thunderwasm.assembler.emitter.JumpCondition;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64GeU extends PlainNumeric implements ProcessedInstruction {
    public static final I64GeU INSTANCE = new I64GeU();

    private I64GeU() {
        super("i64.ge_u", (byte) 0x5A);
    }

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I64);
        context.getFrameState().popOperand(NumberType.I64);
        return this;
    }

    @Override
    public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
        CommonBytecodeGenerator.convertTop2Unsigned(context.getEmitter(), NumberType.I64);
        context.getEmitter().op(Op.LCMP);
        CommonBytecodeGenerator.evalConditionZeroOrOne(
                context.getEmitter(),
                JumpCondition.INT_GREATER_THAN_OR_EQUAL_ZERO
        );
    }

    @Override
    public void processOutputs(CodeEmitContext context) throws WasmAssemblerException {
        context.getFrameState().pushOperand(NumberType.I32);
    }
}
