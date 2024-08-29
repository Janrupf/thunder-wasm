package net.janrupf.thunderwasm.instructions.data;

import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class GlobalIndexData extends IndexData {
    public GlobalIndexData(int index) {
        super(index);
    }

    /**
     * Reads a {@link GlobalIndexData} from the given {@link WasmLoader}.
     *
     * @param loader the loader to read the data from
     * @return the read data
     * @throws IOException if an I/O error occurs
     */
    public static GlobalIndexData read(WasmLoader loader) throws IOException {
        return new GlobalIndexData(loader.readU32());
    }
}
