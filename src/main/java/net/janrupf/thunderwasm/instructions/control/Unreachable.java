package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.WasmLoader;

public final class Unreachable extends WasmInstruction<EmptyInstructionData> {
    public static final Unreachable INSTANCE = new Unreachable();

    private Unreachable() {
        super("unreachable", (byte) 0x00);
    }

    @Override
    public EmptyInstructionData readData(WasmLoader loader) {
        return EmptyInstructionData.INSTANCE;
    }
}
