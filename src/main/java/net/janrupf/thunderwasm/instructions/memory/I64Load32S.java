package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64Load32S extends PlainMemoryLoad {
    public static final I64Load32S INSTANCE = new I64Load32S();

    public I64Load32S() {
        super("i64.load32_s", (byte) 0x34, NumberType.I64, LoadType.SIGNED_32);
    }
}
