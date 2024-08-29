package net.janrupf.thunderwasm.instructions.data;

import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class DataIndexData extends IndexData {
    public DataIndexData(int index) {
        super(index);
    }

    /**
     * Reads a {@link DataIndexData} from the given {@link WasmLoader}.
     *
     * @param loader the loader to read the data from
     * @return the read data
     * @throws IOException if an I/O error occurs
     */
    public static DataIndexData read(WasmLoader loader) throws IOException {
        return new DataIndexData(loader.readU32());
    }
}
