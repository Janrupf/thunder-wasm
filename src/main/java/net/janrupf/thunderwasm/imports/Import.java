package net.janrupf.thunderwasm.imports;

/**
 * Import of a WebAssembly module.
 */
public final class Import {
    private final String module;
    private final String name;
    private final ImportDescription description;

    public Import(String module, String name, ImportDescription description) {
        this.module = module;
        this.name = name;
        this.description = description;
    }

    /**
     * Retrieves the name of the module being imported.
     *
     * @return the module name
     */
    public String getModule() {
        return module;
    }

    /**
     * Retrieves the name of the import.
     *
     * @return the import name
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the description of the import.
     *
     * @return the import description
     */
    public ImportDescription getDescription() {
        return description;
    }
}
