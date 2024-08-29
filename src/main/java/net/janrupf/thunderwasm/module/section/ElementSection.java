package net.janrupf.thunderwasm.module.section;

import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.section.segment.ElementSegment;

public final class ElementSection extends WasmSection {
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
