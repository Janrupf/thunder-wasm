package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeLabel;
import net.janrupf.thunderwasm.assembler.emitter.JumpCondition;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.control.internal.BlockHelper;
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

        WasmFrameState branched = context.getFrameState().branch();

        CodeLabel zeroLabel = context.getEmitter().newLabel();
        context.getEmitter().jump(JumpCondition.INT_EQUAL_ZERO, zeroLabel);

        // If not zero, branch to the indicated label block
        BlockHelper.emitBlockReturn(context, data.getLabelIndex());

        context.restoreFrameStateAfterBranch(branched);
        context.getEmitter().resolveLabel(zeroLabel);
    }
}
