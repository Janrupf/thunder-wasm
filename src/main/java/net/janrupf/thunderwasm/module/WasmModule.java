package net.janrupf.thunderwasm.module;

import net.janrupf.thunderwasm.module.section.WasmSection;

import java.util.Collections;
import java.util.List;

public class WasmModule {
    private final int version;
    private final List<WasmSection> sections;

    WasmModule(int version, List<WasmSection> sections) {
        this.version = version;
        this.sections = sections;
    }

    /**
     * Retrieves the version of the WebAssembly module.
     * <p>
     * The currently only supported version is 1.
     *
     * @return the version of the WebAssembly module
     */
    public int getVersion() {
        return version;
    }

    /**
     * Retrieves the sections of the WebAssembly module.
     *
     * @return the sections of the WebAssembly module
     */
    public List<WasmSection> getSections() {
        return Collections.unmodifiableList(sections);
    }
}
