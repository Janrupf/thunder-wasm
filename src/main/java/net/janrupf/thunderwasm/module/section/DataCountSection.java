package net.janrupf.thunderwasm.module.section;

import net.janrupf.thunderwasm.lookup.SectionLocator;

public class DataCountSection extends WasmSection {
    public static final SectionLocator<DataCountSection> LOCATOR = SectionLocator.of(DataCountSection.class, (byte) 0x0C);

    private final int count;

    public DataCountSection(byte id, int count) {
        super(id);
        this.count = count;
    }

    /**
     * Retrieves the count of the data segments.
     *
     * @return the count of the data segments
     */
    public int getCount() {
        return count;
    }
}
