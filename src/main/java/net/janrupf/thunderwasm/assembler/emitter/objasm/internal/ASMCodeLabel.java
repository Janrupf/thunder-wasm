package net.janrupf.thunderwasm.assembler.emitter.objasm.internal;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeLabel;
import org.objectweb.asm.Label;

public final class ASMCodeLabel implements CodeLabel {
    private final Label inner;
    private boolean resolved;

    public ASMCodeLabel() {
        this.inner = new Label();
        this.resolved = false;
    }

    /**
     * Mark the label as resolved.
     */
    public void markResolved() {
        this.resolved = true;
    }

    /**
     * Check if the label is resolved.
     *
     * @throws WasmAssemblerException if the label is already resolved
     */
    public void checkNotResolved() throws WasmAssemblerException {
        if (this.resolved) {
            throw new WasmAssemblerException("Label is already resolved");
        }
    }

    /**
     * Retrieves the real label object.
     *
     * @return the real label object
     */
    public Label getInner() {
        return inner;
    }
}
