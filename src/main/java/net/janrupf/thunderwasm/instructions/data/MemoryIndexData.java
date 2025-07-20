package net.janrupf.thunderwasm.instructions.data;

import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class MemoryIndexData extends IndexData {
    public MemoryIndexData(int index) {
        super(index);
    }

    /**
     * Reads a {@link MemoryIndexData} from the given {@link WasmLoader}.
     *
     * @param loader the loader to read the data from
     * @return the read data
     * @throws IOException if an I/O error occurs
     */
    public static MemoryIndexData read(WasmLoader loader) throws IOException {
        return new MemoryIndexData(loader.readU32());
    }
}
