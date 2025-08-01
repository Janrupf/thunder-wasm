package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.emitter.frame.JavaFrameSnapshot;

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

    /**
     * Forcefully override the attached frame snapshot.
     * <p>
     * This may be useful in cases where locals need to be discarded
     * and the automatic tracking doesn't work.
     *
     * @param snapshot the new frame snapshot
     */
    void overrideFrameSnapshot(JavaFrameSnapshot snapshot);
}
