package net.janrupf.thunderwasm.instructions.data;

import net.janrupf.thunderwasm.instructions.WasmInstruction;

public abstract class IndexData implements WasmInstruction.Data {
    private final int index;

    public IndexData(int index) {
        this.index = index;
    }

    /**
     * Retrieves the index.
     *
     * @return the index
     */
    public final int getIndex() {
        return index;
    }

    @Override
    public final String toString() {
        return Integer.toUnsignedString(index);
    }
}
