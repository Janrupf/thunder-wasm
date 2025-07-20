package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryStore;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64Store32 extends PlainMemoryStore {
    public static final I64Store32 INSTANCE = new I64Store32();

    public I64Store32() {
        super("i64.store16", (byte) 0x3E, NumberType.I64, StoreType.BIT_32);
    }
}
