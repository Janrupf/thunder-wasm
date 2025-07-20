package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryStore;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32Store8 extends PlainMemoryStore {
    public static final I32Store8 INSTANCE = new I32Store8();

    public I32Store8() {
        super("i32.store8", (byte) 0x3A, NumberType.I32, StoreType.BIT_8);
    }
}
