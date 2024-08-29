package net.janrupf.thunderwasm.module.section;

import net.janrupf.thunderwasm.lookup.SectionLocator;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.types.MemoryType;

/**
 * Represents the memory section of a WebAssembly module.
 */
public final class MemorySection extends WasmSection {
    public static final SectionLocator<MemorySection> LOCATOR = SectionLocator.of(MemorySection.class, (byte) 0x05);

    private final LargeArray<MemoryType> types;

    public MemorySection(byte id, LargeArray<MemoryType> types) {
        super(id);
        this.types = types;
    }

    /**
     * Retrieve the types of the memories.
     *
     * @return the types of the memories
     */
    public LargeArray<MemoryType> getTypes() {
        return types;
    }
}
