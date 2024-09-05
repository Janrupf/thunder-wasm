package net.janrupf.thunderwasm.eval;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.types.ValueType;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the state of the evaluation frame.
 */
public final class EvalFrameState {
    private final List<StackEntry> stack;

    public EvalFrameState() {
        this.stack = new ArrayList<>();
    }

    /**
     * Push a value onto the evaluation stack.
     *
     * @param type  the type of the value
     * @param value the value
     */
    public void push(ValueType type, Object value) {
        stack.add(new StackEntry(type, value));
    }

    /**
     * Pop a value from the evaluation stack.
     *
     * @return the value
     * @throws WasmAssemblerException if the stack is empty
     */
    public StackEntry pop() throws WasmAssemblerException {
        if (stack.isEmpty()) {
            throw new WasmAssemblerException("Stack underflow");
        }

        return stack.remove(stack.size() - 1);
    }

    /**
     * Retrieves the size of the evaluation stack.
     *
     * @return the stack size
     */
    public int getStackSize() {
        return stack.size();
    }
}
