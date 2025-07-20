package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32Load8S extends PlainMemoryLoad {
    public static final I32Load8S INSTANCE = new I32Load8S();

    public I32Load8S() {
        super("i32.load8_s", (byte) 0x2C, NumberType.I32, LoadType.SIGNED_8);
    }
}
