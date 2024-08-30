package net.janrupf.thunderwasm.imports;

/**
 * Import of a WebAssembly module.
 */
public final class Import<T extends ImportDescription> {
    private final String module;
    private final String name;
    private final T description;

    public Import(String module, String name, T description) {
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
    public T getDescription() {
        return description;
    }

    /**
     * Try to cast the description to a specific type.
     *
     * @param clazz the class to cast to
     * @param <N>   the type to cast to
     * @return the casted import or null if the cast failed
     */
    @SuppressWarnings("unchecked")
    public <N extends ImportDescription> Import<N> tryCast(Class<N> clazz) {
        if (clazz.isInstance(description)) {
            return (Import<N>) this;
        } else {
            return null;
        }
    }
}
