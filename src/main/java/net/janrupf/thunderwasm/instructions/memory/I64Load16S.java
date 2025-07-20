package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64Load16S extends PlainMemoryLoad {
    public static final I64Load16S INSTANCE = new I64Load16S();

    public I64Load16S() {
        super("i64.load16_s", (byte) 0x32, NumberType.I64, LoadType.SIGNED_16);
    }
}
