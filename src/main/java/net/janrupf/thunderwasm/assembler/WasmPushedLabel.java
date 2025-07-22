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

    public WasmPushedLabel(CodeLabel label, LargeArray<ValueType> stackOperands) {
        this.label = label;
        this.stackOperands = stackOperands;
    }

    /**
     * Retrieves the underlying code label that represents the jump point.
     *
     * @return the underlying code label
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
}
