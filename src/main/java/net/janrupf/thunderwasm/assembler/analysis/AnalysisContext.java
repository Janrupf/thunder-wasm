package net.janrupf.thunderwasm.assembler.analysis;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.instructions.Expr;
import net.janrupf.thunderwasm.instructions.InstructionInstance;
import net.janrupf.thunderwasm.util.ObjectUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Context for performing code analysis.
 */
public final class AnalysisContext {
    private final Expr currentExpr;
    private final LocalVariableUsage localVariableUsage;
    private final AnalysisContext parent;

    private final List<AnalysisContext> subContexts;
    private WasmAssemblerException analysisException;
    private boolean wasRun;

    private boolean usesDirectReturn;

    private AnalysisContext(
            Expr expr,
            LocalVariableUsage localVariableUsage,
            AnalysisContext parent
    ) {
        this.currentExpr = expr;
        this.localVariableUsage = localVariableUsage;
        this.parent = parent;

        this.subContexts = new ArrayList<>();
        this.analysisException = null;
        this.wasRun = false;
        this.usesDirectReturn = false;
    }

    /**
     * Run the code analysis if required.
     *
     * @throws WasmAssemblerException if the analysis fails
     */
    public void run() throws WasmAssemblerException {
        if (this.wasRun) {
            if (analysisException != null) {
                throw analysisException;
            }

            return;
        }

        try {
            for (InstructionInstance instance : this.currentExpr.getInstructions()) {
                instance.getInstruction().runAnalysis(this, ObjectUtil.forceCast(instance.getData()));
            }
        } catch (WasmAssemblerException e) {
            this.analysisException = e;
        } finally {
            this.wasRun = true;
        }
    }

    /**
     * Retrieve the local variable usage tracker.
     *
     * @return the local variable usage tracker
     */
    public LocalVariableUsage getLocalVariableUsage() {
        return localVariableUsage;
    }

    /**
     * Mark the expression as using direct return.
     */
    public void markForDirectReturn() {
        this.usesDirectReturn = true;
        if (parent != null) {
            parent.markForDirectReturn();
        }
    }

    /**
     * Determines whether the expression uses direct returns.
     *
     * @return true if the expression uses direct returns, false otherwise
     */
    public boolean usesDirectReturn() {
        return this.usesDirectReturn;
    }

    /**
     * Branch the analysis context for a sub expression.
     *
     * @param expr the subexpression
     * @return the new analysis context
     */
    public AnalysisContext branchForExpression(Expr expr) {
        LocalVariableUsage subUsage = new LocalVariableUsage(localVariableUsage);

        AnalysisContext subcontext = new AnalysisContext(expr, subUsage, this);
        subContexts.add(subcontext);

        return subcontext;
    }

    /**
     * Retrieve the expression being analyzed.
     *
     * @return the expression being analyzed
     */
    public Expr getCurrentExpr() {
        return currentExpr;
    }

    /**
     * Retrieve the sub contexts.
     *
     * @return the subcontexts
     */
    List<AnalysisContext> getSubContexts() {
        return subContexts;
    }

    /**
     * Create a new top-level analysis context for a function.
     *
     * @param expr the expression of the function
     * @return the created context
     */
    public static AnalysisContext createForFunction(Expr expr) {
        return new AnalysisContext(
                expr,
                new LocalVariableUsage(null),
                null
        );
    }
}
