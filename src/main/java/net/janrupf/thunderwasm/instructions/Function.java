package net.janrupf.thunderwasm.instructions;

import net.janrupf.thunderwasm.module.encoding.LargeArray;

public final class Function {
    private final Expr expr;
    private final LargeArray<Local> locals;

    public Function(Expr expr, LargeArray<Local> locals) {
        this.expr = expr;
        this.locals = locals;
    }

    /**
     * Retrieves the function body as an expression.
     *
     * @return the function body
     */
    public Expr getExpr() {
        return expr;
    }

    /**
     * Retrieves the local variables of the function.
     *
     * @return the local variables of the function
     */
    public LargeArray<Local> getLocals() {
        return locals;
    }
}
