package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64Load32U extends PlainMemoryLoad {
    public static final I64Load32U INSTANCE = new I64Load32U();

    public I64Load32U() {
        super("i64.load32_u", (byte) 0x35, NumberType.I64, LoadType.UNSIGNED_32);
    }
}
