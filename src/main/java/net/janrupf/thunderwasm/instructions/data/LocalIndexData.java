package net.janrupf.thunderwasm.instructions.data;

import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class LocalIndexData extends IndexData {
    public LocalIndexData(int index) {
        super(index);
    }

    /**
     * Reads a {@link LocalIndexData} from the given {@link WasmLoader}.
     *
     * @param loader the loader to read the data from
     * @return the read data
     * @throws IOException if an I/O error occurs
     */
    public static LocalIndexData read(WasmLoader loader) throws IOException {
        return new LocalIndexData(loader.readU32());
    }
}
