package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.InvokeType;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.WasmLoader;

public final class Unreachable extends WasmInstruction<EmptyInstructionData> implements ProcessedInstruction {
    private static final ObjectType ILLEGAL_STATE_EXCEPTION_TYPE = ObjectType.of(IllegalStateException.class);

    public static final Unreachable INSTANCE = new Unreachable();

    private Unreachable() {
        super("unreachable", (byte) 0x00);
    }

    @Override
    public EmptyInstructionData readData(WasmLoader loader) {
        return EmptyInstructionData.INSTANCE;
    }

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        return this;
    }

    @Override
    public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        emitter.doNew(ILLEGAL_STATE_EXCEPTION_TYPE);
        emitter.duplicate();
        emitter.loadConstant("Unreachable instruction executed");
        emitter.invoke(
                ILLEGAL_STATE_EXCEPTION_TYPE,
                "<init>",
                new JavaType[]{ ObjectType.of(String.class) },
                PrimitiveType.VOID,
                InvokeType.SPECIAL,
                false
        );

        emitter.op(Op.THROW);
    }

    @Override
    public void processOutputs(CodeEmitContext context) throws WasmAssemblerException {
        context.getFrameState().markUnreachable();
    }
}
