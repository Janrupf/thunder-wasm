package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32Load8U extends PlainMemoryLoad {
    public static final I32Load8U INSTANCE = new I32Load8U();

    public I32Load8U() {
        super("i32.load8_u", (byte) 0x2D, NumberType.I32, LoadType.UNSIGNED_8);
    }
}
