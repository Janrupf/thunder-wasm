package net.janrupf.thunderwasm.instructions.numeric.internal;

import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public abstract class PlainNumeric extends WasmInstruction<EmptyInstructionData> {
    protected PlainNumeric(String name, byte opCode) {
        super(name, opCode);
    }

    @Override
    public final EmptyInstructionData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return EmptyInstructionData.INSTANCE;
    }
}
