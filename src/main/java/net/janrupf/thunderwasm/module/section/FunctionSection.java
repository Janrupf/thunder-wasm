package net.janrupf.thunderwasm.module.section;

import net.janrupf.thunderwasm.lookup.SectionLocator;
import net.janrupf.thunderwasm.module.encoding.LargeIntArray;

/**
 * Represents the function section of a WebAssembly module.
 */
public final class FunctionSection extends WasmSection {
    public static final SectionLocator<FunctionSection> LOCATOR = SectionLocator.of(FunctionSection.class, (byte) 0x03);

    private final LargeIntArray types;

    public FunctionSection(byte id, LargeIntArray types) {
        super(id);
        this.types = types;
    }

    /**
     * Retrieve the types of the functions.
     *
     * @return the types of the functions
     */
    public LargeIntArray getTypes() {
        return types;
    }
}
