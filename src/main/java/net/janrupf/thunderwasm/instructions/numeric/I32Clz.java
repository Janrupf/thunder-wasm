package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.InvokeType;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32Clz extends PlainNumeric implements ProcessedInstruction {
    public static final I32Clz INSTANCE = new I32Clz();

    private I32Clz() {
        super("i32.clz", (byte) 0x67);
    }

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I32);
        return this;
    }

    @Override
    public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
        context.getEmitter().invoke(
                ObjectType.of(Integer.class),
                "numberOfLeadingZeros",
                new JavaType[]{PrimitiveType.INT},
                PrimitiveType.INT,
                InvokeType.STATIC,
                false
        );
    }

    @Override
    public void processOutputs(CodeEmitContext context) throws WasmAssemblerException {
        context.getFrameState().pushOperand(NumberType.I32);
    }
}
