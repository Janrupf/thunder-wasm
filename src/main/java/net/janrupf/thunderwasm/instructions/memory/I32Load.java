package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32Load extends PlainMemoryLoad {
    public static final I32Load INSTANCE = new I32Load();

    public I32Load() {
        super("i32.load", (byte) 0x28, NumberType.I32, LoadType.NATIVE);
    }
}
