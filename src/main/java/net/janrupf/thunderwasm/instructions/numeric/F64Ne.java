package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class F64Ne extends PlainNumeric {
    public static final F64Ne INSTANCE = new F64Ne();

    private F64Ne() {
        super("f64.ne", (byte) 0x62);
    }

    @Override
    public void emitCode(
            CodeEmitContext context, EmptyInstructionData data
    ) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        frameState.popOperand(NumberType.F64);
        frameState.popOperand(NumberType.F64);
        emitter.op(Op.DCMPG);
        frameState.pushOperand(NumberType.I32);
        CommonBytecodeGenerator.evalCompResultZeroOrOne(frameState, emitter, ComparisonResult.NOT_EQUAL);
    }
}
