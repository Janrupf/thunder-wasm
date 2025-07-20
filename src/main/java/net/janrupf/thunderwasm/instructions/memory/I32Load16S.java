package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32Load16S extends PlainMemoryLoad {
    public static final I32Load16S INSTANCE = new I32Load16S();

    public I32Load16S() {
        super("i32.load16_s", (byte) 0x2E, NumberType.I32, LoadType.SIGNED_16);
    }
}
