package net.janrupf.thunderwasm.module.section;

import net.janrupf.thunderwasm.lookup.SectionLocator;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.types.TableType;

/**
 * Represents the table section of a WebAssembly module.
 */
public final class TableSection extends WasmSection {
    public static final SectionLocator<TableSection> LOCATOR = SectionLocator.of(TableSection.class, (byte) 0x04);

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
