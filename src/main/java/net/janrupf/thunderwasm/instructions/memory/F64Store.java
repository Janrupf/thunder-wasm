package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryStore;
import net.janrupf.thunderwasm.types.NumberType;

public final class F64Store extends PlainMemoryStore {
    public static final F64Store INSTANCE = new F64Store();

    public F64Store() {
        super("f64.store", (byte) 0x39, NumberType.F64, StoreType.NATIVE);
    }
}
