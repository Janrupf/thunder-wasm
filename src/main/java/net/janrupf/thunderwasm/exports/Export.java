package net.janrupf.thunderwasm.exports;

/**
 * Export of a WebAssembly module.
 */
public final class Export {
    private final String name;
    private final ExportDescription description;

    public Export(String name, ExportDescription description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Retrieves the name of the export.
     *
     * @return the export name
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the description of the export.
     *
     * @return the export description
     */
    public ExportDescription getDescription() {
        return description;
    }
}
