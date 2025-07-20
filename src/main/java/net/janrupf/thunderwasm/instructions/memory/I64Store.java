package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryStore;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64Store extends PlainMemoryStore {
    public static final I64Store INSTANCE = new I64Store();

    public I64Store() {
        super("i64.store", (byte) 0x37, NumberType.I64, StoreType.NATIVE);
    }
}
