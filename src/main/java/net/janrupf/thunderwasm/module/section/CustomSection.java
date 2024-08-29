package net.janrupf.thunderwasm.module.section;

import net.janrupf.thunderwasm.module.encoding.LargeByteArray;

public final class CustomSection extends WasmSection {
    private final String name;
    private final LargeByteArray data;

    public CustomSection(byte id, String name, LargeByteArray data) {
        super(id);
        this.name = name;
        this.data = data;
    }

    /**
     * Retrieves the name of the section.
     *
     * @return the name of the section
     */
    public String getName() {
        return name;
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
