package net.janrupf.thunderwasm.instructions;

import net.janrupf.thunderwasm.module.InvalidModuleException;

/**
 * Thrown when an unknown instruction is encountered
 */
public class UnknownInstructionException extends InvalidModuleException {
    public UnknownInstructionException(byte opcode) {
        super("Unknown instruction with opcode 0x" + Integer.toUnsignedString(((int) opcode) & 0xFF, 16));
    }
}
