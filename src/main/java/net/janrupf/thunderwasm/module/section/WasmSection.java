package net.janrupf.thunderwasm.module.section;

/**
 * Represents a WebAssembly section.
 */
public abstract class WasmSection {
    private final byte id;

    public WasmSection(byte id) {
        this.id = id;
    }

    /**
     * Retrieves the id of the section.
     *
     * @return the id of the section
     */
    public byte getId() {
        return id;
    }
}
