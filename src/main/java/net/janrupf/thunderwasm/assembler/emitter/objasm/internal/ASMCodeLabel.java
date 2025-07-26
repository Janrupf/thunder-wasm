package net.janrupf.thunderwasm.assembler.emitter.objasm.internal;

import net.janrupf.thunderwasm.assembler.JavaFrameSnapshot;
import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeLabel;
import org.objectweb.asm.Label;

public final class ASMCodeLabel implements CodeLabel {
    private final Label inner;
    private boolean resolved;

    private JavaFrameSnapshot knownFrameSnapshot;
    private Throwable resolvedAt;

    public ASMCodeLabel() {
        this.inner = new Label();
        this.resolved = false;
        this.resolvedAt = null;
    }

    /**
     * Mark the label as resolved.
     */
    public void markResolved() {
        this.resolved = true;
        // this.resolvedAt = new Throwable("LABEL FIRST RESOLVED HERE");
    }

    /**
     * Check if the label is resolved.
     *
     * @throws WasmAssemblerException if the label is already resolved
     */
    public void checkNotResolved() throws WasmAssemblerException {
        if (this.resolved) {
            throw new WasmAssemblerException("Label is already resolved, see cause for where", resolvedAt);
        }
    }


    /**
     * Attach a known frame state snapshot to this label.
     *
     * @param snapshot the snapshot to attach
     * @throws WasmAssemblerException if there is already a snapshot attached and its incompatible
     */
    public void attachFrameState(JavaFrameSnapshot snapshot) throws WasmAssemblerException {
        if (this.knownFrameSnapshot != null) {
            this.knownFrameSnapshot.checkCompatible(snapshot);
        } else {
            this.knownFrameSnapshot = snapshot;
        }
    }

    /**
     * Retrieve the snapshot that is attached to this label.
     *
     * @return the snapshot attached to this label, or null, if none yet
     */
    public JavaFrameSnapshot getKnownFrameSnapshot() {
        return knownFrameSnapshot;
    }

    /**
     * Retrieves the real label object.
     *
     * @return the real label object
     */
    public Label getInner() {
        return inner;
    }

    @Override
    public boolean isReachable() {
        return this.knownFrameSnapshot != null;
    }
}
