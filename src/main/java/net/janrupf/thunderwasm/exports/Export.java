package net.janrupf.thunderwasm.exports;

/**
 * Export of a WebAssembly module.
 */
public final class Export<T extends ExportDescription> {
    private final String name;
    private final T description;

    public Export(String name, T description) {
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
    public <N extends ExportDescription> Export<N> tryCast(Class<N> clazz) {
        if (clazz.isInstance(description)) {
            return (Export<N>) this;
        } else {
            return null;
        }
    }
}
