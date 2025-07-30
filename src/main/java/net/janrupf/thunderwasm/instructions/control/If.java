package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.analysis.AnalysisContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.CodeLabel;
import net.janrupf.thunderwasm.assembler.emitter.JumpCondition;
import net.janrupf.thunderwasm.instructions.Expr;
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
    public void emitCode(CodeEmitContext context, BlockData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I32);

        CodeEmitter emitter = context.getEmitter();

        Expr falseExpr = data.getSecondaryExpression();

        CodeLabel endLabel = emitter.newLabel();
        CodeLabel falseLabel = falseExpr == null ? endLabel : emitter.newLabel();

        WasmFrameState beforeFirstBranch = context.getFrameState().branch();

        emitter.jump(JumpCondition.INT_EQUAL_ZERO, falseLabel);

        BlockHelper.emitInvokeSplitBlock(context, data, true, false);

        if (falseExpr != null) {
            if (context.getFrameState().isReachable()) {
                emitter.jump(JumpCondition.ALWAYS, endLabel);
            }
            emitter.resolveLabel(falseLabel);

            // Reset the stack state to before the branch, it never happened in this timeline
            context.restoreFrameStateAfterBranch(beforeFirstBranch);

            BlockHelper.emitInvokeSplitBlock(context, data, false, false);
        }

        emitter.resolveLabel(endLabel);

        if (endLabel.isReachable()) {
            context.getFrameState().markReachable();
        }
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
