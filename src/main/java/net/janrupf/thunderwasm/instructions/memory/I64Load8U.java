package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64Load8U extends PlainMemoryLoad {
    public static final I64Load8U INSTANCE = new I64Load8U();

    public I64Load8U() {
        super("i64.load8_u", (byte) 0x31, NumberType.I64, LoadType.UNSIGNED_8);
    }
}
