package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.types.NumberType;

public final class F32Load extends PlainMemoryLoad {
    public static final F32Load INSTANCE = new F32Load();

    public F32Load() {
        super("f32.load", (byte) 0x2A, NumberType.F32, LoadType.NATIVE);
    }
}
