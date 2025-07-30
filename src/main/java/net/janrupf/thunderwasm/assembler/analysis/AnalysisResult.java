package net.janrupf.thunderwasm.assembler.analysis;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.instructions.Expr;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public final class AnalysisResult {
    private final Map<Expr, LocalVariableUsage> localVariableUsage;
    private final Set<Expr> directReturns;

    private AnalysisResult() {
        this.localVariableUsage = new IdentityHashMap<>();
        this.directReturns = Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private void processContext(AnalysisContext analysisContext) {
        for (AnalysisContext subcontext : analysisContext.getSubContexts()) {
            processContext(subcontext);
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
     * Compile the result from the given analysis context.
     *
     * @param analysisContext the context to compile the result from
     * @return the compiled result
     */
    public static AnalysisResult compileFromContext(AnalysisContext analysisContext) {
        AnalysisResult result = new AnalysisResult();
        result.processContext(analysisContext);

        return result;
    }
}
