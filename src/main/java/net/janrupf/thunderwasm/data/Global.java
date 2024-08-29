package net.janrupf.thunderwasm.data;

import net.janrupf.thunderwasm.instructions.Expr;
import net.janrupf.thunderwasm.types.GlobalType;

/**
 * Represents a global variable.
 */
public final class Global {
    private final GlobalType type;
    private final Expr init;

    public Global(GlobalType type, Expr init) {
        this.type = type;
        this.init = init;
    }

    /**
     * Retrieves the type of the global.
     *
     * @return the global type
     */
    public GlobalType getType() {
        return type;
    }

    /**
     * Retrieves the initialization expression of the global.
     *
     * @return the initialization expression
     */
    public Expr getInit() {
        return init;
    }
}
