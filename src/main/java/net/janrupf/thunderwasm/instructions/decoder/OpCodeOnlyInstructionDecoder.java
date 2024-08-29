package net.janrupf.thunderwasm.instructions.decoder;

import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;

/**
 * Decodes an instruction which is only identified by its opcode.
 */
public final class OpCodeOnlyInstructionDecoder extends InstructionDecoder {
    private final WasmInstruction<?> instruction;

    public OpCodeOnlyInstructionDecoder(WasmInstruction<?> instruction) {
        this.instruction = instruction;
    }

    @Override
    public WasmInstruction<?> decode(byte opCode, WasmLoader loader) throws InvalidModuleException {
        if (opCode != instruction.getOpCode()) {
            throw new InvalidModuleException(
                    "Expected opcode 0x" + Integer.toUnsignedString(((int) instruction.getOpCode()) & 0xFF, 16)
                            + " but got 0x" + Integer.toUnsignedString(((int) opCode) & 0xFF, 16));
        }

        return instruction;
    }

    @Override
    public boolean canDecode(WasmInstruction<?> instruction) {
        return this.instruction.equals(instruction);
    }
}
