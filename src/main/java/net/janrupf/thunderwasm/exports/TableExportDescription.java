package net.janrupf.thunderwasm.exports;

/**
 * Describes an export of a table.
 */
public final class TableExportDescription implements ExportDescription {
    private final int index;

    public TableExportDescription(int index) {
        this.index = index;
    }

    /**
     * Retrieves the index of the table being exported.
     *
     * @return the table index
     */
    public int getIndex() {
        return index;
    }
}
