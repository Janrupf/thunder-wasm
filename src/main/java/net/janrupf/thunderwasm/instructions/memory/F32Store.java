package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryStore;
import net.janrupf.thunderwasm.types.NumberType;

public final class F32Store extends PlainMemoryStore {
    public static final F32Store INSTANCE = new F32Store();

    public F32Store() {
        super("f32.store", (byte) 0x38, NumberType.F32, StoreType.NATIVE);
    }
}
