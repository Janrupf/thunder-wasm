package net.janrupf.thunderwasm.assembler;

import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;

import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JavaFrameSnapshot)) return false;
        JavaFrameSnapshot that = (JavaFrameSnapshot) o;
        return stackSlotSize == that.stackSlotSize && localsSlotSize == that.localsSlotSize && Objects.equals(stack, that.stack) && Objects.equals(locals, that.locals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stackSlotSize, stack, localsSlotSize, locals);
    }

    /**
     * Check that this snapshot is compatible with another snapshot.
     *
     * @param other the snapshot to check against
     * @throws WasmAssemblerException if the snapshots are not compatible
     */
    public void checkCompatible(JavaFrameSnapshot other) throws WasmAssemblerException {
        boolean compatible = this.stack.equals(other.stack) && this.locals.equals(other.locals);

        if (!compatible) {
            throw new WasmAssemblerException("Stack frames incompatible:\n" + format() + "\n===\n" + other.format());
        }
    }

    public String format() {
        StringBuilder localsBuilder = new StringBuilder("Locals: {");
        for (int i = 0; i < locals.size(); i++) {
            if (i != 0) {
                localsBuilder.append(", ");
            }

            localsBuilder.append(locals.get(i).toJvmDescriptor());
        }
        localsBuilder.append("}");

        StringBuilder stackBuilder = new StringBuilder("Stack: {");
        for (int i = 0; i < stack.size(); i++) {
            if (i != 0) {
                stackBuilder.append(", ");
            }

            stackBuilder.append(stack.get(i).toJvmDescriptor());
        }
        stackBuilder.append("}");

        return localsBuilder + "\n" + stackBuilder;
    }
}
