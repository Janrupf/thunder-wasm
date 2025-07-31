package net.janrupf.thunderwasm.assembler.analysis;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.instructions.Expr;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public final class AnalysisResult {
    private static final int BLOCK_SPLIT_INSTRUCTION_THRESHOLD = 5000;
    private static final int BLOCK_SPLIT_INSTRUCTION_OVER_ALLOWANCE = 100;
    private static final int BLOCK_SPLIT_DEPTH_THRESHOLD = 20;

    private final Map<Expr, LocalVariableUsage> localVariableUsage;
    private final Set<Expr> directReturns;
    private final Set<Expr> blockSplitTargets;

    private AnalysisResult() {
        this.localVariableUsage = new IdentityHashMap<>();
        this.directReturns = Collections.newSetFromMap(new IdentityHashMap<>());
        this.blockSplitTargets = Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private void processContext(AnalysisContext analysisContext, int unsplitInstructionCount, int depth) {
        int ownSize = analysisContext.getCurrentExpr().getInstructions().size();
        int totalInstructionCount = ownSize + unsplitInstructionCount;

        boolean doBlockSplit = totalInstructionCount >= BLOCK_SPLIT_INSTRUCTION_THRESHOLD &&
                (ownSize > BLOCK_SPLIT_INSTRUCTION_OVER_ALLOWANCE ||
                        totalInstructionCount > BLOCK_SPLIT_INSTRUCTION_THRESHOLD + BLOCK_SPLIT_INSTRUCTION_OVER_ALLOWANCE) || (depth >= BLOCK_SPLIT_DEPTH_THRESHOLD);

        if (doBlockSplit) {
            blockSplitTargets.add(analysisContext.getCurrentExpr());
        }

        for (AnalysisContext subcontext : analysisContext.getSubContexts()) {
            processContext(subcontext, doBlockSplit ? 0 : totalInstructionCount, doBlockSplit ? 0 : depth + 1);
        }

        this.localVariableUsage.put(analysisContext.getCurrentExpr(), analysisContext.getLocalVariableUsage());

        if (analysisContext.usesDirectReturn()) {
            this.directReturns.add(analysisContext.getCurrentExpr());
        }
    }

    /**
     * Look up the local variable usage for an expression.
     *
     * @param expr the expression to look the usage up for
     * @return the local variable usage for that expression
     * @throws WasmAssemblerException if there is no information about local usage available
     */
    public LocalVariableUsage getLocalVariableUsage(Expr expr) throws WasmAssemblerException {
        if (!this.localVariableUsage.containsKey(expr)) {
            throw new WasmAssemblerException("Local variable analysis has not been performed for the requested expression");
        }

        return this.localVariableUsage.get(expr);
    }

    /**
     * Determine whether an expression uses direct returns.
     *
     * @param expr the expression to check
     * @return true if the expression uses direct returns, false otherwise
     */
    public boolean usesDirectReturn(Expr expr) {
        return this.directReturns.contains(expr);
    }

    /**
     * Determine whether the expression block should be split into an extra method.
     *
     * @param expr the expression to check
     * @return true if the block should be split, false otherwise
     */
    public boolean shouldSplitBlock(Expr expr) {
        return this.blockSplitTargets.contains(expr);
    }

    /**
     * Compile the result from the given analysis context.
     *
     * @param analysisContext the context to compile the result from
     * @return the compiled result
     */
    public static AnalysisResult compileFromContext(AnalysisContext analysisContext) {
        AnalysisResult result = new AnalysisResult();
        result.processContext(analysisContext, 0, 0);

        return result;
    }
}
