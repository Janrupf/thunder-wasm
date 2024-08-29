package net.janrupf.thunderwasm.module.section;

import net.janrupf.thunderwasm.lookup.SectionLocator;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.section.segment.DataSegment;

public final class DataSection extends WasmSection {
    public static final SectionLocator<DataSection> LOCATOR = SectionLocator.of(DataSection.class, (byte) 0x0B);

    private final LargeArray<DataSegment> segments;

    public DataSection(byte id, LargeArray<DataSegment> segments) {
        super(id);
        this.segments = segments;
    }

    /**
     * Retrieves the segments of this data section.
     *
     * @return the segments
     */
    public LargeArray<DataSegment> getSegments() {
        return segments;
    }
}
