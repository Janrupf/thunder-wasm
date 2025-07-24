package net.janrupf.thunderwasm.module.section;

import net.janrupf.thunderwasm.exports.Export;
import net.janrupf.thunderwasm.lookup.SectionLocator;
import net.janrupf.thunderwasm.module.encoding.LargeArray;

/**
 * Represents the export section of a WebAssembly module.
 */
public final class ExportSection extends WasmSection {
    public static final SectionLocator<ExportSection> LOCATOR = SectionLocator.of(ExportSection.class, (byte) 0x07);

    private final LargeArray<Export<?>> exports;

    public ExportSection(byte id, LargeArray<Export<?>> exports) {
        super(id);
        this.exports = exports;
    }

    /**
     * Retrieves the exports of the module.
     *
     * @return the exports
     */
    public LargeArray<Export<?>> getExports() {
        return exports;
    }
}
