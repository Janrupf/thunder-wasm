package net.janrupf.thunderwasm.instructions.data;

import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class ElementIndexData extends IndexData {
    public ElementIndexData(int index) {
        super(index);
    }

    /**
     * Reads a {@link ElementIndexData} from the given {@link WasmLoader}.
     *
     * @param loader the loader to read the data from
     * @return the read data
     * @throws IOException if an I/O error occurs
     */
    public static ElementIndexData read(WasmLoader loader) throws IOException {
        return new ElementIndexData(loader.readU32());
    }
}
