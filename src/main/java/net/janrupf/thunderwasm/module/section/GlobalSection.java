package net.janrupf.thunderwasm.module.section;

import net.janrupf.thunderwasm.data.Global;
import net.janrupf.thunderwasm.lookup.SectionLocator;
import net.janrupf.thunderwasm.module.encoding.LargeArray;

public final class GlobalSection extends WasmSection {
    public static final SectionLocator<GlobalSection> LOCATOR = SectionLocator.of(GlobalSection.class, (byte) 0x06);

    private final LargeArray<Global> globals;

    public GlobalSection(byte id, LargeArray<Global> globals) {
        super(id);
        this.globals = globals;
    }

    /**
     * Retrieves the globals of the section.
     *
     * @return the globals
     */
    public LargeArray<Global> getGlobals() {
        return globals;
    }
}
