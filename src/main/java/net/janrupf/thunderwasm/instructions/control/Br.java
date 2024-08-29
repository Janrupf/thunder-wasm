package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class Br extends WasmInstruction<LabelData> {
    public static final Br INSTANCE = new Br();

    private Br() {
        super("br", (byte) 0x0C);
    }

    @Override
    public LabelData readData(WasmLoader loader) throws IOException {
        return LabelData.read(loader);
    }
}
