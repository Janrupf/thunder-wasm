package net.janrupf.thunderwasm.module.section;

import net.janrupf.thunderwasm.lookup.SectionLocator;

/**
 * Represents the start section of a WebAssembly module.
 */
public final class StartSection extends WasmSection {
    public static final SectionLocator<StartSection> LOCATOR = SectionLocator.of(StartSection.class, (byte) 0x08);

    private final int index;

    public StartSection(byte id, int index) {
        super(id);
        this.index = index;
    }

    /**
     * Retrieves the index of the function to start.
     *
     * @return the function index
     */
    public int getIndex() {
        return index;
    }
}
