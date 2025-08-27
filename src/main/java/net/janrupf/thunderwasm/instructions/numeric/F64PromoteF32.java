package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class F64PromoteF32 extends PlainNumeric implements ProcessedInstruction {
    public static final F64PromoteF32 INSTANCE = new F64PromoteF32();

    private F64PromoteF32() {
        super("f64.promote_f32", (byte) 0xBB);
    }

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.F32);
        return this;
    }

    @Override
    public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
        context.getEmitter().op(Op.F2D);
    }

    @Override
    public void processOutputs(CodeEmitContext context) throws WasmAssemblerException {
        context.getFrameState().pushOperand(NumberType.F64);
    }
}
