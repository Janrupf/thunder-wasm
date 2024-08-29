package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class Block extends WasmInstruction<BlockData> {
    public static final Block INSTANCE = new Block();

    private Block() {
        super("block", (byte) 0x02);
    }

    @Override
    public BlockData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return BlockData.read(loader, false);
    }
}
