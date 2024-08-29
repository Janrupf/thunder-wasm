package net.janrupf.thunderwasm.module.section;

import net.janrupf.thunderwasm.instructions.Function;
import net.janrupf.thunderwasm.lookup.SectionLocator;
import net.janrupf.thunderwasm.module.encoding.LargeArray;

public final class CodeSection extends WasmSection {
    public static final SectionLocator<CodeSection> LOCATOR = SectionLocator.of(CodeSection.class, (byte) 0x0A);

    private final LargeArray<Function> functions;

    public CodeSection(byte id, LargeArray<Function> functions) {
        super(id);
        this.functions = functions;
    }

    /**
     * Retrieves the functions of the code section.
     *
     * @return the functions of the code section
     */
    public LargeArray<Function> getFunctions() {
        return functions;
    }
}
