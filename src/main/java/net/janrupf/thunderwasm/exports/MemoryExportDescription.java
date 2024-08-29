package net.janrupf.thunderwasm.exports;

/**
 * Describes an export of a memory.
 */
public final class MemoryExportDescription implements ExportDescription {
    private final int index;

    public MemoryExportDescription(int index) {
        this.index = index;
    }

    /**
     * Retrieves the index of the memory being exported.
     *
     * @return the memory index
     */
    public int getIndex() {
        return index;
    }
}
