package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class BrIf extends WasmInstruction<LabelData> {
    public static final BrIf INSTANCE = new BrIf();

    private BrIf() {
        super("br_if", (byte) 0x0D);
    }

    @Override
    public LabelData readData(WasmLoader loader) throws IOException {
        return LabelData.read(loader);
    }
}
