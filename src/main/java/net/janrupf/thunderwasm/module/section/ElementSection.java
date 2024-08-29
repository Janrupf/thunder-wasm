package net.janrupf.thunderwasm.module.section;

import net.janrupf.thunderwasm.lookup.SectionLocator;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.section.segment.ElementSegment;

public final class ElementSection extends WasmSection {
    public static final SectionLocator<ElementSection> LOCATOR = SectionLocator.of(ElementSection.class, (byte) 0x09);

    private final LargeArray<ElementSegment> segments;

    public ElementSection(byte id, LargeArray<ElementSegment> segments) {
        super(id);
        this.segments = segments;
    }

    /**
     * Retrieves the segments of this element section.
     *
     * @return the segments
     */
    public LargeArray<ElementSegment> getSegments() {
        return segments;
    }
}
