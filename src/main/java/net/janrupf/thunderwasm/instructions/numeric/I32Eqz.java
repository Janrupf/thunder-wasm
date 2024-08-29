package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CommonBytecodeGenerator;
import net.janrupf.thunderwasm.assembler.emitter.JumpCondition;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32Eqz extends PlainNumeric {
    public static final I32Eqz INSTANCE = new I32Eqz();

    private I32Eqz() {
        super("i32.eqz", (byte) 0x45);
    }

    @Override
    public void emitCode(
            CodeEmitContext context, EmptyInstructionData data
    ) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I32);
        CommonBytecodeGenerator.evalConditionZeroOrOne(
                context.getFrameState(),
                context.getEmitter(),
                JumpCondition.INT_EQUAL_ZERO
        );
    }
}
