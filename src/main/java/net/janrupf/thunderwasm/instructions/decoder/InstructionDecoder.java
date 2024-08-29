package net.janrupf.thunderwasm.instructions.decoder;

import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public abstract class InstructionDecoder {
    /**
     * Decodes an instruction from the given opcode.
     *
     * @param opCode the opcode of the instruction
     * @param loader the loader to read the data from
     * @return the instruction
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public abstract WasmInstruction<?> decode(byte opCode, WasmLoader loader)
            throws IOException, InvalidModuleException;

    /**
     * Determines whether the decoder can decode the given instruction.
     *
     * @param instruction the instruction to check
     * @return whether the decoder can decode the instruction
     */
    public abstract boolean canDecode(WasmInstruction<?> instruction);

    /**
     * Merges this decoder with another decoder.
     * <p>
     * Only some decoders can be merged. If this decoder cannot be merged with the other decoder, an
     * {@link UnsupportedOperationException} is thrown.
     *
     * @param other the other decoder
     * @return the merged decoder
     */
    public InstructionDecoder merge(InstructionDecoder other) {
        throw new UnsupportedOperationException(
                "A decoder of type " + getClass().getName() + " cannot be merged with a decoder of type "
                        + other.getClass().getName()
        );
    }

    /**
     * Constructs a new {@link OpCodeOnlyInstructionDecoder} for the given instruction.
     *
     * @param instruction the instruction to decode
     * @return the decoder
     */
    public static OpCodeOnlyInstructionDecoder opCodeOnly(WasmInstruction<?> instruction) {
        return new OpCodeOnlyInstructionDecoder(instruction);
    }

    /**
     * Constructs a new {@link U32VariantInstructionDecoder} for the given instruction.
     *
     * @param variant     the variant of the instruction
     * @param instruction the instruction to decode
     * @return the decoder
     */
    public static U32VariantInstructionDecoder u32Variant(int variant, WasmInstruction<?> instruction) {
        return new U32VariantInstructionDecoder(variant, instruction);
    }
}
