package net.janrupf.thunderwasm.module.section;

import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.types.TableType;

/**
 * Represents the table section of a WebAssembly module.
 */
public final class TableSection extends WasmSection {
    private final LargeArray<TableType> types;

    public TableSection(byte id, LargeArray<TableType> types) {
        super(id);
        this.types = types;
    }

    /**
     * Retrieve the types of the tables.
     *
     * @return the types of the tables
     */
    public LargeArray<TableType> getTypes() {
        return types;
    }
}
