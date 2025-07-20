package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64Load extends PlainMemoryLoad {
    public static final I64Load INSTANCE = new I64Load();

    public I64Load() {
        super("i64.load", (byte) 0x29, NumberType.I64, LoadType.NATIVE);
    }
}
