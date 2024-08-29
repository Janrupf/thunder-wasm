package net.janrupf.thunderwasm.imports;

import net.janrupf.thunderwasm.types.GlobalType;

/**
 * Import description for a global.
 */
public final class GlobalImportDescription implements ImportDescription {
    private final GlobalType type;

    public GlobalImportDescription(GlobalType type) {
        this.type = type;
    }

    /**
     * Retrieves the type of the global being imported.
     *
     * @return the global type
     */
    public GlobalType getType() {
        return type;
    }
}
