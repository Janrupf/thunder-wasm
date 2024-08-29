package net.janrupf.thunderwasm.imports;

/**
 * Describes an import of a (function) type.
 */
public final class TypeImportDescription implements ImportDescription {
    private final int index;

    public TypeImportDescription(int index) {
        this.index = index;
    }

    /**
     * Retrieves the index of the type being imported.
     *
     * @return the type index
     */
    public int getIndex() {
        return index;
    }
}
