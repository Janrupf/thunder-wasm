package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryStore;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32Store16 extends PlainMemoryStore {
    public static final I32Store16 INSTANCE = new I32Store16();

    public I32Store16() {
        super("i32.store16", (byte) 0x3B, NumberType.I32, StoreType.BIT_16);
    }
}
