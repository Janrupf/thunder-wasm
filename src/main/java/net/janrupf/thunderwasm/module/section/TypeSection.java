package net.janrupf.thunderwasm.module.section;

import net.janrupf.thunderwasm.lookup.SectionLocator;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.types.FunctionType;

public final class TypeSection extends WasmSection {
    public static final SectionLocator<TypeSection> LOCATOR = SectionLocator.of(TypeSection.class, (byte) 0x01);

    private final LargeArray<FunctionType> types;

    public TypeSection(byte id, LargeArray<FunctionType> types) {
        super(id);
        this.types = types;
    }

    /**
     * Retrieves the types of the section.
     *
     * @return the types of the section
     */
    public LargeArray<FunctionType> getTypes() {
        return types;
    }
}
