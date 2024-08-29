package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class LabelData implements WasmInstruction.Data {
    private final int labelIndex;

    private LabelData(int labelIndex) {
        this.labelIndex = labelIndex;
    }

    /**
     * Retrieves the index of the label.
     *
     * @return the index of the label
     */
    public int getLabelIndex() {
        return labelIndex;
    }

    @Override
    public String toString() {
        return "@" + labelIndex;
    }

    /**
     * Reads a label data from the given loader.
     *
     * @param loader the loader to read the data from
     * @return the label data
     * @throws IOException if an I/O error occurs
     */
    public static LabelData read(WasmLoader loader) throws IOException {
        return new LabelData(loader.readU32());
    }
}
