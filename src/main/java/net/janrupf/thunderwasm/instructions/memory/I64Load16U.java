package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64Load16U extends PlainMemoryLoad {
    public static final I64Load16U INSTANCE = new I64Load16U();

    public I64Load16U() {
        super("i64.load16_u", (byte) 0x33, NumberType.I64, LoadType.UNSIGNED_16);
    }
}
