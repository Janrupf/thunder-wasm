package net.janrupf.thunderwasm.module.section.segment;

import net.janrupf.thunderwasm.module.encoding.LargeByteArray;

public final class DataSegment {
    private final LargeByteArray init;
    private final DataSegmentMode mode;

    public DataSegment(LargeByteArray init, DataSegmentMode mode) {
        this.init = init;
        this.mode = mode;
    }

    /**
     * Retrieves the initialization data of the data segment.
     *
     * @return the initialization data
     */
    public LargeByteArray getInit() {
        return init;
    }

    /**
     * Retrieves the mode of the data segment.
     *
     * @return the mode
     */
    public DataSegmentMode getMode() {
        return mode;
    }
}
