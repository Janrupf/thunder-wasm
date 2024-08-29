package net.janrupf.thunderwasm.module.section;

import net.janrupf.thunderwasm.imports.Import;
import net.janrupf.thunderwasm.module.encoding.LargeArray;

/**
 * Represents the import section of a WebAssembly module.
 */
public final class ImportSection extends WasmSection {
    private final LargeArray<Import> imports;

    public ImportSection(byte id, LargeArray<Import> imports) {
        super(id);
        this.imports = imports;
    }

    /**
     * Retrieves the imports of this section.
     *
     * @return the imports
     */
    public LargeArray<Import> getImports() {
        return imports;
    }
}
