package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.WasmU32VariantInstruction;
import net.janrupf.thunderwasm.instructions.data.DataIndexData;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class DataDrop extends WasmU32VariantInstruction<DataIndexData> {
    public static final DataDrop INSTANCE = new DataDrop();

    private DataDrop() {
        super("data.drop", (byte) 0xFC, 9);
    }

    @Override
    public DataIndexData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return DataIndexData.read(loader);
    }
}
