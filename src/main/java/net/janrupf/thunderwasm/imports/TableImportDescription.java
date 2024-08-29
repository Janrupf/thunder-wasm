package net.janrupf.thunderwasm.imports;

import net.janrupf.thunderwasm.types.TableType;

/**
 * Import description for a table.
 */
public final class TableImportDescription implements ImportDescription {
    private final TableType type;

    public TableImportDescription(TableType type) {
        this.type = type;
    }

    /**
     * Retrieves the type of the table being imported.
     *
     * @return the table type
     */
    public TableType getType() {
        return type;
    }
}
