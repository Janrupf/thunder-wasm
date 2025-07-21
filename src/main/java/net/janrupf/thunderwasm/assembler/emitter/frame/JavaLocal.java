package net.janrupf.thunderwasm.assembler.emitter.frame;

import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;

import java.util.Objects;

/**
 * Tracks an allocated local.
 */
public final class JavaLocal {
    private JavaStackFrameState frameState;

    private final int slot;
    private final JavaType type;

    JavaLocal(JavaStackFrameState frameState, int slot, JavaType type) {
        this.frameState = frameState;
        this.slot = slot;
        this.type = type;
    }

    /**
     * The slot index this local is in.
     *
     * @return the slot index
     */
    public int getSlot() {
        return slot;
    }

    /**
     * The type this local holds.
     *
     * @return the local type
     */
    public JavaType getType() {
        return type;
    }

    /**
     * Determine whether this local is still valid.
     *
     * @return true if this local is still valid, false otherwise
     */
    public boolean isValid() {
        return this.frameState != null;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JavaLocal)) return false;
        JavaLocal javaLocal = (JavaLocal) o;
        return slot == javaLocal.slot && Objects.equals(frameState, javaLocal.frameState) && Objects.equals(type, javaLocal.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(frameState, slot, type);
    }

    /**
     * Free the local
     */
    public void free() {
        this.frameState.freeLocal(this);
        this.frameState = null;
    }

    @Override
    public String toString() {
        String annotation = "";
        if (!isValid()) {
            annotation = " (invalid)";
        }

        return type + "@" + slot + annotation;
    }
}
