package net.janrupf.thunderwasm.exports;

/**
 * Describes an export of a function.
 */
public final class FunctionExportDescription implements ExportDescription {
    private final int index;

    public FunctionExportDescription(int index) {
        this.index = index;
    }

    /**
     * Retrieves the index of the function being exported.
     *
     * @return the function index
     */
    public int getIndex() {
        return index;
    }
}
