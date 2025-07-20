package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64Load8S extends PlainMemoryLoad {
    public static final I64Load8S INSTANCE = new I64Load8S();

    public I64Load8S() {
        super("i64.load8_s", (byte) 0x30, NumberType.I64, LoadType.SIGNED_8);
    }
}
