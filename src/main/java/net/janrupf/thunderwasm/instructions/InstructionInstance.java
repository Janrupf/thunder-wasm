package net.janrupf.thunderwasm.instructions;

/**
 * Represents an instance of an instruction with its data.
 */
public final class InstructionInstance {
    private final WasmInstruction<?> instruction;
    private final WasmInstruction.Data data;

    public <T extends WasmInstruction.Data> InstructionInstance(WasmInstruction<T> instruction, T data) {
        this.instruction = instruction;
        this.data = data;
    }

    /**
     * Retrieves the actual instruction of this instance.
     *
     * @return the instruction
     */
    public WasmInstruction<?> getInstruction() {
        return instruction;
    }

    /**
     * Retrieves the data of this instruction instance.
     *
     * @return the data
     */
    public WasmInstruction.Data getData() {
        return data;
    }

    @Override
    public String toString() {
        String dataStr = data.toString();
        if (dataStr.isEmpty()) {
            return instruction.getName();
        }

        return instruction.getName() + " " + dataStr;
    }
}
