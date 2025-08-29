package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeLabel;
import net.janrupf.thunderwasm.assembler.emitter.JumpCondition;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.control.internal.BlockHelper;
import net.janrupf.thunderwasm.instructions.control.internal.ControlHelper;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ValueType;

import java.io.IOException;
import java.util.List;

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
    public ProcessedInstruction processInputs(CodeEmitContext context, LabelData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I32);

        final int labelIndex = data.getLabelIndex();

        List<ValueType> branchLabelArity = BlockHelper.validateBlockReturn(context, labelIndex);

        // This is required in order to properly work with the polymorphic stack - we essentially
        // transform the potentially polymorphic stack into a concrete one by pushing the
        // branch label arity values onto the stack, which are then popped again in the
        // branched-to block, ensuring that the stack is concrete at that point.
        //
        // Ideally this would happen in processOutputs, but the BlockHelper methods require
        // the stack to contain the values that need to be kept when branching, so we
        // have to do it here.
        for (int i = branchLabelArity.size() - 1; i >= 0; i--) {
            context.getFrameState().popOperand(branchLabelArity.get(i));
        }
        context.getFrameState().pushAllOperands(branchLabelArity);

        return new ProcessedInstruction() {
            @Override
            public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
                WasmFrameState branched = context.getFrameState().branch();

                CodeLabel zeroLabel = context.getEmitter().newLabel();
                context.getEmitter().jump(JumpCondition.INT_EQUAL_ZERO, zeroLabel);

                // If not zero, branch to the indicated label block
                BlockHelper.emitBlockReturn(context, labelIndex);

                context.restoreFrameStateAfterBranch(branched);
                context.getEmitter().resolveLabel(zeroLabel);
            }

            @Override
            public void processOutputs(CodeEmitContext context) {
            }
        };
    }
}
