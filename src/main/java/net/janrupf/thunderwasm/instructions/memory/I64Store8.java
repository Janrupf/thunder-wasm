package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryStore;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64Store8 extends PlainMemoryStore {
    public static final I64Store8 INSTANCE = new I64Store8();

    public I64Store8() {
        super("i64.store8", (byte) 0x3C, NumberType.I64, StoreType.BIT_8);
    }
}
