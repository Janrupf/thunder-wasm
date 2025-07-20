package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.types.NumberType;

public final class F64Load extends PlainMemoryLoad {
    public static final F64Load INSTANCE = new F64Load();

    public F64Load() {
        super("f64.load", (byte) 0x2B, NumberType.F64, LoadType.NATIVE);
    }
}

