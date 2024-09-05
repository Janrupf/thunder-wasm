package net.janrupf.thunderwasm.instructions.data;

import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;

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

    /**
     * Converts the index to a {@link LargeArrayIndex}.
     *
     * @return the converted index
     */
    public LargeArrayIndex toArrayIndex() {
        return LargeArrayIndex.fromU32(index);
    }

    @Override
    public final String toString() {
        return Integer.toUnsignedString(index);
    }
}
