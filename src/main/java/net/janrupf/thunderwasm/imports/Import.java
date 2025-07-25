package net.janrupf.thunderwasm.imports;

/**
 * Import of a WebAssembly module.
 */
public final class Import<T extends ImportDescription> {
    private final String module;
    private final String name;
    private final T description;
    private final int counter;

    public Import(String module, String name, T description, int counter) {
        this.module = module;
        this.name = name;
        this.description = description;
        this.counter = counter;
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
     * Retrieves the import index counter.
     * <p>
     * This is mainly useful for generating unique import names.
     * Having the same import twice is valid, but creates a situation where
     * naming the field becomes difficult. Thus we also have this counter,
     * which has no semantic meaning but helps uniquely identifying imports.
     *
     * @return the import counter
     */
    public int getCounter() {
        return counter;
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
