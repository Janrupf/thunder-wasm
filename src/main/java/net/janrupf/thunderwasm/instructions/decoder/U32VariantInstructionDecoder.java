package net.janrupf.thunderwasm.instructions.decoder;

import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Decodes an instruction which is identified by a u32 variant.
 */
public final class U32VariantInstructionDecoder extends InstructionDecoder {
    private final Map<Integer, WasmInstruction<?>> knownInstructions;

    public U32VariantInstructionDecoder(int variant, WasmInstruction<?> instruction) {
        this(Collections.singletonMap(variant, instruction));
    }

    private U32VariantInstructionDecoder(Map<Integer, WasmInstruction<?>> knownInstructions) {
        this.knownInstructions = knownInstructions;
    }

    @Override
    public WasmInstruction<?> decode(byte opCode, WasmLoader loader) throws IOException, InvalidModuleException {
        int variant = loader.readU32();
        WasmInstruction<?> instruction = knownInstructions.get(variant);

        if (instruction == null) {
            throw new InvalidModuleException("Unknown instruction variant " + Integer.toUnsignedString(variant) + " for opcode 0x" +
                    Integer.toUnsignedString(((int) opCode) & 0xFF, 16));
        } else if (instruction.getOpCode() != opCode) {
            throw new InvalidModuleException(
                    "Expected opcode 0x" + Integer.toUnsignedString(((int) instruction.getOpCode()) & 0xFF, 16) +
                            " but got 0x" + Integer.toUnsignedString(((int) opCode) & 0xFF, 16) +
                            " for variant " + Integer.toUnsignedString(variant));
        }

        return instruction;
    }

    @Override
    public boolean canDecode(WasmInstruction<?> instruction) {
        return knownInstructions.containsValue(instruction);
    }

    @Override
    public InstructionDecoder merge(InstructionDecoder other) {
        if (!(other instanceof U32VariantInstructionDecoder)) {
            throw new UnsupportedOperationException(
                    "Can only merge a U32VariantInstructionDecoder with another U32VariantInstructionDecoder"
            );
        }

        Map<Integer, WasmInstruction<?>> merged = new HashMap<>(knownInstructions);
        merged.putAll(((U32VariantInstructionDecoder) other).knownInstructions);

        return new U32VariantInstructionDecoder(merged);
    }
}
