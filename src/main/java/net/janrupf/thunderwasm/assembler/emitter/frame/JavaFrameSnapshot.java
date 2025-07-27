package net.janrupf.thunderwasm.assembler.emitter.frame;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;

import java.util.List;
import java.util.Objects;

/**
 * Represents a frozen state of the current java stack.
 */
public final class JavaFrameSnapshot {
    private final List<JavaType> stack;
    private final List<JavaLocalSlot> locals;

    public JavaFrameSnapshot(
            List<JavaType> stack,
            List<JavaLocalSlot> locals
    ) {
        this.stack = stack;
        this.locals = locals;
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
     * Retrieves the state of the locals.
     *
     * @return the state of the locals
     */
    public List<JavaLocalSlot> getLocals() {
        return locals;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JavaFrameSnapshot)) return false;
        JavaFrameSnapshot that = (JavaFrameSnapshot) o;
        return Objects.equals(stack, that.stack) && Objects.equals(locals, that.locals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stack, locals);
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

            JavaLocalSlot slot = locals.get(i);

            if (slot instanceof JavaLocalSlot.Used) {
                localsBuilder.append(((JavaLocalSlot.Used) slot).getLocal().getType().toJvmDescriptor());
            } else if (slot instanceof JavaLocalSlot.Vacant) {
                localsBuilder.append("<EMPTY>");
            } else if (slot instanceof JavaLocalSlot.Continuation) {
                localsBuilder.append("<CONTINUATION>");
            }
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
