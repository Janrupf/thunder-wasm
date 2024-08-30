package net.janrupf.thunderwasm.module.section;

import net.janrupf.thunderwasm.imports.Import;
import net.janrupf.thunderwasm.lookup.SectionLocator;
import net.janrupf.thunderwasm.module.encoding.LargeArray;

/**
 * Represents the import section of a WebAssembly module.
 */
public final class ImportSection extends WasmSection {
    public static final SectionLocator<ImportSection> LOCATOR = SectionLocator.of(ImportSection.class, (byte) 0x02);

    private final LargeArray<Import<?>> imports;

    public ImportSection(byte id, LargeArray<Import<?>> imports) {
        super(id);
        this.imports = imports;
    }

    /**
     * Retrieves the imports of this section.
     *
     * @return the imports
     */
    public LargeArray<Import<?>> getImports() {
        return imports;
    }
}
