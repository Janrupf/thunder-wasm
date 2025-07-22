package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeLabel;
import net.janrupf.thunderwasm.assembler.emitter.JumpCondition;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.control.internal.ControlHelper;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.NumberType;

import java.io.IOException;

public final class BrIf extends WasmInstruction<LabelData> {
    public static final BrIf INSTANCE = new BrIf();

    private BrIf() {
        super("br_if", (byte) 0x0D);
    }

    @Override
    public LabelData readData(WasmLoader loader) throws IOException {
        return LabelData.read(loader);
    }

    @Override
    public void emitCode(CodeEmitContext context, LabelData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I32);

        CodeLabel zeroLabel = context.getEmitter().newLabel();
        context.getEmitter().jump(JumpCondition.INT_EQUAL_ZERO, zeroLabel);

        // If not zero, branch to the indicated label block
        CodeLabel branchTarget = ControlHelper.emitCleanStackForBlockLabel(context, data.getLabelIndex());
        context.getEmitter().jump(JumpCondition.ALWAYS, branchTarget);

        context.getEmitter().resolveLabel(zeroLabel);
    }
}
