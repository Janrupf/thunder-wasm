package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class Loop extends WasmInstruction<BlockData> {
    public static final Loop INSTANCE = new Loop();

    private Loop() {
        super("loop", (byte) 0x03);
    }

    @Override
    public BlockData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return BlockData.read(loader, false);
    }
}
