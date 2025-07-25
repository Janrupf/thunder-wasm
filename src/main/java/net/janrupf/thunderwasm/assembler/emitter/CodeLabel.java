package net.janrupf.thunderwasm.assembler.emitter;

/**
 * Marker interface for labels in the code.
 */
public interface CodeLabel {
    /**
     * Determine whether the label is reachable according to current knowledge.
     * <p>
     * This mostly depends on whether the label has been resolved
     * and whether it was targeted by some jump.
     *
     * @return true if the label is reachable, false otherwise
     */
    boolean isReachable();
}
