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

public final class I64Eqz extends PlainNumeric implements ProcessedInstruction {
    public static final I64Eqz INSTANCE = new I64Eqz();

    private I64Eqz() {
        super("i64.eqz", (byte) 0x50);
    }

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I64);
        return this;
    }

    @Override
    public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
        context.getEmitter().loadConstant(0L);
        context.getEmitter().op(Op.LCMP);
        CommonBytecodeGenerator.evalConditionZeroOrOne(
                context.getEmitter(),
                JumpCondition.INT_EQUAL_ZERO
        );
    }

    @Override
    public void processOutputs(CodeEmitContext context) throws WasmAssemblerException {
        context.getFrameState().pushOperand(NumberType.I32);
    }
}
