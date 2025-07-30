package net.janrupf.thunderwasm.assembler;

import net.janrupf.thunderwasm.assembler.emitter.CodeLabel;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.types.ValueType;

/**
 * Represents a label that has been pushed.
 */
public final class WasmPushedLabel {
    private final CodeLabel label;
    private final LargeArray<ValueType> stackOperands;
    private final boolean isNonLocal;

    private boolean used;

    public WasmPushedLabel(CodeLabel label, LargeArray<ValueType> stackOperands, boolean isNonLocal) {
        this.label = label;
        this.stackOperands = stackOperands;
        this.isNonLocal = isNonLocal;
    }

    /**
     * Mark the label as having been used.
     */
    public void markUsed() {
        this.used = true;
    }

    /**
     * Determine whether the label is in used.
     *
     * @return true if the label is in use, false otherwise
     */
    public boolean isUsed() {
        return this.used;
    }

    /**
     * Retrieves the underlying code label that represents the jump point.
     *
     * @return the underlying code label, or null, if this is a non-local label
     */
    public CodeLabel getCodeLabel() {
        return label;
    }

    /**
     * Retrieves the stack operands at the labels location.
     *
     * @return the local stack operands
     */
    public LargeArray<ValueType> getStackOperands() {
        return stackOperands;
    }

    /**
     * Determines whether this pushed label is a non-local label.
     * <p>
     * A non-local label is a label which branch target is not inside the current
     * method. This mostly happens when using split blocks, where an inner block
     * may jump to an outer block
     *
     * @return true if this is a non-local label, false otherwise
     */
    public boolean isNonLocal() {
        return isNonLocal;
    }
}
