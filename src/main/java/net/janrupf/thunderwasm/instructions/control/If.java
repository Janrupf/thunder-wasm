package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class If extends WasmInstruction<BlockData> {
    public static final If INSTANCE = new If();

    public If() {
        super("if", (byte) 0x04);
    }

    @Override
    public BlockData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return BlockData.read(loader, true);
    }
}
