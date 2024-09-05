package net.janrupf.thunderwasm.instructions.data;

import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class TableIndexData extends IndexData {
    public TableIndexData(int index) {
        super(index);
    }

    /**
     * Reads a {@link TableIndexData} from the given {@link WasmLoader}.
     *
     * @param loader the loader to read the data from
     * @return the read data
     * @throws IOException if an I/O error occurs
     */
    public static TableIndexData read(WasmLoader loader) throws IOException {
        return new TableIndexData(loader.readU32());
    }
}
