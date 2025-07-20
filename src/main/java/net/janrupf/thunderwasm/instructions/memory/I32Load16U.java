package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32Load16U extends PlainMemoryLoad {
    public static final I32Load16U INSTANCE = new I32Load16U();

    public I32Load16U() {
        super("i32.load16_u", (byte) 0x2F, NumberType.I32, LoadType.UNSIGNED_16);
    }
}
