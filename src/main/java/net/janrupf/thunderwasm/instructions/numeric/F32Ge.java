package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.ComparisonResult;
import net.janrupf.thunderwasm.assembler.emitter.CommonBytecodeGenerator;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;

public final class F32Ge extends PlainNumeric {
    public static final F32Ge INSTANCE = new F32Ge();

    private F32Ge() {
        super("f32.ge", (byte) 0x60);
    }

    @Override
    public void emitCode(
            CodeEmitContext context, EmptyInstructionData data
    ) throws WasmAssemblerException {
        CommonBytecodeGenerator.evalFloatCompNaNZero(
                context.getFrameState(),
                context.getEmitter(),
                ComparisonResult.GREATER_THAN_OR_EQUAL
        );
    }
}
