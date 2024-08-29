package net.janrupf.thunderwasm.assembler;

import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;

import java.util.List;

/**
 * Represents a frozen state of the current java stack.
 */
public final class JavaFrameSnapshot {
    private final int stackSlotSize;
    private final List<JavaType> stack;
    private final int localsSlotSize;
    private final List<JavaType> locals;

    public JavaFrameSnapshot(
            int stackSlotSize,
            List<JavaType> stack,
            int localsSlotSize,
            List<JavaType> locals
    ) {
        this.stackSlotSize = stackSlotSize;
        this.stack = stack;
        this.localsSlotSize = localsSlotSize;
        this.locals = locals;
    }

    /**
     * Retrieve the size of the stack in slots.
     *
     * @return the size of the stack in slots
     */
    public int getStackSlotSize() {
        return stackSlotSize;
    }

    /**
     * Retrieves the state of the stack.
     *
     * @return the state of the stack
     */
    public List<JavaType> getStack() {
        return stack;
    }

    /**
     * Retrieve the size of the locals in slots.
     *
     * @return the size of the locals in slots
     */
    public int getLocalsSlotSize() {
        return localsSlotSize;
    }

    /**
     * Retrieves the state of the locals.
     *
     * @return the state of the locals
     */
    public List<JavaType> getLocals() {
        return locals;
    }
}
