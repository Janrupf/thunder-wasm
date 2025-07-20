package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryStore;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64Store16 extends PlainMemoryStore {
    public static final I64Store16 INSTANCE = new I64Store16();

    public I64Store16() {
        super("i64.store16", (byte) 0x3D, NumberType.I64, StoreType.BIT_16);
    }
}
