package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.analysis.AnalysisContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.CodeLabel;
import net.janrupf.thunderwasm.assembler.emitter.JumpCondition;
import net.janrupf.thunderwasm.instructions.Expr;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.control.internal.BlockHelper;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.NumberType;

import java.io.IOException;

public final class If extends WasmInstruction<BlockData> {
    public static final If INSTANCE = new If();

    public If() {
        super("if", (byte) 0x04);
    }

    @Override
    public BlockData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return BlockData.read(loader, true);
    }

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, BlockData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I32);

        // This is a bit hacky, but the nature of the if instruction with its 2 control flows makes it hard to
        // fit into the standard model of instruction processing. So we just handle everything here.
        return new ProcessedInstruction() {
            private BlockHelper.ProcessedBlock processBranch(CodeEmitContext context, boolean primary)
                    throws WasmAssemblerException {
                return BlockHelper.processBlockInputs(context, data, primary, false);
            }

            @Override
            public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
                CodeEmitter emitter = context.getEmitter();

                Expr falseExpr = data.getSecondaryExpression();

                CodeLabel endLabel = emitter.newLabel();
                CodeLabel falseLabel = falseExpr == null ? endLabel : emitter.newLabel();

                WasmFrameState beforeFirstBranch = context.getFrameState().branch();

                emitter.jump(JumpCondition.INT_EQUAL_ZERO, falseLabel);

                BlockHelper.ProcessedBlock trueBranch = processBranch(context, true);
                trueBranch.emitBytecode(context);
                trueBranch.processOutputs(context);

                if (falseExpr != null) {
                    WasmFrameState trueBranchState = null;

                    if (context.getFrameState().isReachable()) {
                        emitter.jump(JumpCondition.ALWAYS, endLabel);
                        trueBranchState = context.getFrameState().branch();
                    }
                    emitter.resolveLabel(falseLabel);

                    // Reset the stack state to before the branch, it never happened in this timeline
                    context.restoreFrameStateAfterBranch(beforeFirstBranch);

                    BlockHelper.ProcessedBlock falseBranch = processBranch(context, false);
                    falseBranch.emitBytecode(context);
                    falseBranch.processOutputs(context);

                    if (!context.getFrameState().isReachable() && trueBranchState != null) {
                        // If the second branch is not reachable, but the first one was, we need to restore the state
                        // to what it was after the first branch
                        context.restoreFrameStateAfterBranch(trueBranchState);
                    }
                } else {
                    // Mainly required for correct reachability analysis - an if with only a true branch
                    // doesn't change reachability of the current frame state
                    context.restoreFrameStateAfterBranch(beforeFirstBranch);
                }

                emitter.resolveLabel(endLabel);
            }

            @Override
            public void processUnreachable(CodeEmitContext context) throws WasmAssemblerException {
                WasmFrameState beforeFirstBranch = context.getFrameState().branch();

                BlockHelper.ProcessedBlock trueBranch = processBranch(context, true);
                trueBranch.processUnreachable(context);
                trueBranch.processOutputs(context);

                if (data.getSecondaryExpression() != null) {
                    context.restoreFrameStateAfterBranch(beforeFirstBranch);

                    BlockHelper.ProcessedBlock falseBranch = processBranch(context, false);
                    falseBranch.processUnreachable(context);
                    falseBranch.processOutputs(context);
                }
            }

            @Override
            public void processOutputs(CodeEmitContext context) {
            }
        };
    }

    @Override
    public void runAnalysis(AnalysisContext context, BlockData data) throws WasmAssemblerException {
        AnalysisContext primarySubcontext = context.branchForExpression(data.getPrimaryExpression());
        primarySubcontext.run();

        if (data.getSecondaryExpression() != null) {
            AnalysisContext secondarySubcontext = context.branchForExpression(data.getSecondaryExpression());
            secondarySubcontext.run();
        }
    }
}
