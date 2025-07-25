package net.janrupf.thunderwasm.instructions.reference;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CommonBytecodeGenerator;
import net.janrupf.thunderwasm.assembler.emitter.JumpCondition;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;

import java.io.IOException;

public final class RefIsNull extends WasmInstruction<EmptyInstructionData> {
    public static final RefIsNull INSTANCE = new RefIsNull();

    private RefIsNull() {
        super("ref.is_null", (byte) 0xD1);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        ValueType type = context.getFrameState().popAnyOperand();
        if (!(type instanceof ReferenceType)) {
            throw new WasmAssemblerException("Expected reference type on stack");
        }

        CommonBytecodeGenerator.evalConditionZeroOrOne(
                context.getEmitter(),
                JumpCondition.IS_NULL
        );
        context.getFrameState().pushOperand(NumberType.I32);
    }

    @Override
    public EmptyInstructionData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return EmptyInstructionData.INSTANCE;
    }
}
