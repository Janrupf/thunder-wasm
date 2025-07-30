package net.janrupf.thunderwasm.runtime.state;

/**
 * Used by generated code facilitate block branches across split blocks.
 */
public final class BlockReturn {
    private final int branchDepth;
    private final MultiValue stack;
    private final MultiValue locals;

    private BlockReturn(int branchDepth, MultiValue stack, MultiValue locals) {
        this.branchDepth = branchDepth;
        this.stack = stack;
        this.locals = locals;
    }

    public static BlockReturn create(int branchDepth, MultiValue stack, MultiValue locals) {
        return new BlockReturn(branchDepth, stack, locals);
    }

    public int getBranchDepth() {
        return branchDepth;
    }

    public MultiValue getStack() {
        return stack;
    }

    public MultiValue getLocals() {
        return locals;
    }
}
