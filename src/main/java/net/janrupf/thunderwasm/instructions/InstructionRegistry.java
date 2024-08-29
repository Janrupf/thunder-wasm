package net.janrupf.thunderwasm.instructions;

import net.janrupf.thunderwasm.instructions.decoder.InstructionDecoder;

import java.util.HashSet;
import java.util.Set;

public final class InstructionRegistry {
    private final InstructionDecoder[] knownOpCodes;

    public InstructionRegistry() {
        knownOpCodes = new InstructionDecoder[256];
    }

    /**
     * Determine whether the registry has a given instruction.
     *
     * @param instruction the instruction to check for
     * @return whether the instruction is known
     */
    public boolean has(WasmInstruction<?> instruction) {
        InstructionDecoder decoder = lookupDecoder(instruction);
        return decoder != null && decoder.canDecode(instruction);
    }

    /**
     * Register an instruction in the registry.
     *
     * @param instruction the instruction to register
     */
    public void registerInstruction(WasmInstruction<?> instruction) {
        int decoderIndex = decoderIndex(instruction);
        InstructionDecoder newDecoder = instruction.getDecoder();
        InstructionDecoder alreadyRegistered = knownOpCodes[decoderIndex];
        InstructionDecoder mergedDecoder = alreadyRegistered != null ? alreadyRegistered.merge(newDecoder) : newDecoder;

        knownOpCodes[decoderIndex] = mergedDecoder;
    }

    /**
     * Retrieves the decoder for the given instruction.
     *
     * @param opCode the opcode of the instruction
     * @return the instruction
     * @throws UnknownInstructionException if the instruction is unknown
     */
    public InstructionDecoder getDecoder(byte opCode) throws UnknownInstructionException {
        InstructionDecoder decoder = lookupDecoder(opCode);

        if (decoder == null) {
            throw new UnknownInstructionException(opCode);
        }

        return decoder;
    }

    /**
     * Looks up the decoder for the given instruction.
     *
     * @param instruction the instruction
     * @return the decoder
     */
    private InstructionDecoder lookupDecoder(WasmInstruction<?> instruction) {
        return lookupDecoder(instruction.getOpCode());
    }

    /**
     * Looks up the decoder for the given opcode.
     *
     * @param opCode the opcode of the instruction
     * @return the decoder
     */
    private InstructionDecoder lookupDecoder(byte opCode) {
        return knownOpCodes[decoderIndex(opCode)];
    }

    /**
     * Looks up the index in the registry for the given instruction.
     *
     * @param instruction the instruction
     * @return the index
     */
    private int decoderIndex(WasmInstruction<?> instruction) {
        return decoderIndex(instruction.getOpCode());
    }

    /**
     * Looks up the index in the registry for the given opcode.
     *
     * @param opCode the opcode of the instruction
     * @return the index
     */
    private int decoderIndex(byte opCode) {
        return Byte.toUnsignedInt(opCode);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Set<WasmInstruction<?>> instructions;

        private Builder() {
            instructions = new HashSet<>();
        }

        /**
         * Adds all instructions from the given set to the registry.
         *
         * @param set the set of instructions to add
         * @return this builder
         */
        public Builder with(InstructionSet set) {
            instructions.addAll(set.getInstructions());
            return this;
        }

        /**
         * Adds an instruction to the registry.
         *
         * @param instruction the instruction to add
         * @return this builder
         */
        public Builder with(WasmInstruction<?> instruction) {
            instructions.add(instruction);
            return this;
        }

        /**
         * Removes all instructions from the given set from the registry.
         *
         * @param set the set of instructions to remove
         * @return this builder
         */
        public Builder without(InstructionSet set) {
            instructions.removeAll(set.getInstructions());
            return this;
        }

        /**
         * Removes an instruction from the registry.
         *
         * @param instruction the instruction to remove
         * @return this builder
         */
        public Builder without(WasmInstruction<?> instruction) {
            instructions.remove(instruction);
            return this;
        }

        /**
         * Builds the registry.
         *
         * @return the registry
         */
        public InstructionRegistry build() {
            InstructionRegistry registry = new InstructionRegistry();
            instructions.forEach(registry::registerInstruction);
            return registry;
        }
    }
}
