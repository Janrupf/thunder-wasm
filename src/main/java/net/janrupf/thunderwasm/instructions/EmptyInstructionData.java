package net.janrupf.thunderwasm.instructions;

import java.util.Objects;

/**
 * Placeholder data for instructions that do not have any data.
 */
public final class EmptyInstructionData implements WasmInstruction.Data {
    public static final EmptyInstructionData INSTANCE = new EmptyInstructionData();

    private EmptyInstructionData() {}

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass") // Not required, we reference compare with INSTANCE
    @Override
    public boolean equals(Object obj) {
        return obj == INSTANCE;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(0);
    }

    @Override
    public String toString() {
        return "";
    }
}
