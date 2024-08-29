package net.janrupf.thunderwasm.module.section;

import net.janrupf.thunderwasm.module.encoding.LargeByteArray;

public final class UnidentifiedSection extends WasmSection {
    private final LargeByteArray data;

    public UnidentifiedSection(byte id, LargeByteArray data) {
        super(id);
        this.data = data;
    }

    /**
     * Retrieves the data of the section.
     *
     * @return the data of the section
     */
    public LargeByteArray getData() {
        return data;
    }
}
