package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryStore;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32Store extends PlainMemoryStore {
    public static final I32Store INSTANCE = new I32Store();

    public I32Store() {
        super("i32.store", (byte) 0x36, NumberType.I32, StoreType.NATIVE);
    }
}
