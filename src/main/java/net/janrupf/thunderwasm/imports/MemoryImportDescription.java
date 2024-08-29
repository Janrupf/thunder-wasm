package net.janrupf.thunderwasm.imports;

import net.janrupf.thunderwasm.types.MemoryType;

/**
 * Import description for a memory.
 */
public final class MemoryImportDescription implements ImportDescription {
    private final MemoryType type;

    public MemoryImportDescription(MemoryType type) {
        this.type = type;
    }

    /**
     * Retrieves the type of the memory being imported.
     *
     * @return the memory type
     */
    public MemoryType getType() {
        return type;
    }
}
