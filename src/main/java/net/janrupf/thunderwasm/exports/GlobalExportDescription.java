package net.janrupf.thunderwasm.exports;

/**
 * Describes an export of a global.
 */
public final class GlobalExportDescription implements ExportDescription {
    private final int index;

    public GlobalExportDescription(int index) {
        this.index = index;
    }

    /**
     * Retrieves the index of the global being exported.
     *
     * @return the global index
     */
    public int getIndex() {
        return index;
    }
}
