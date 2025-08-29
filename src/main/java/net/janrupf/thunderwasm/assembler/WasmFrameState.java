package net.janrupf.thunderwasm.assembler;

import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.UnknownType;
import net.janrupf.thunderwasm.types.ValueType;

import java.util.*;

public final class WasmFrameState {
    // Could use a java.util.Stack, but its synchronized and uses an
    // array under the hood anyway.
    private final List<ValueType> operandStack;
    private final List<ValueType> locals;
    private final List<ValueType> returnTypes;
    private final List<ValueType> blockReturnTypes;
    private boolean isReachable;
    private boolean isPolymorphic;

    public WasmFrameState() {
        this(
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );
    }

    public WasmFrameState(
            List<ValueType> argumentTypes,
            List<ValueType> locals,
            List<ValueType> returnTypes,
            List<ValueType> blockReturnTypes
    ) {
        this.operandStack = new ArrayList<>();

        this.locals = new ArrayList<>();
        this.locals.addAll(argumentTypes);
        this.locals.addAll(locals);
        this.returnTypes = returnTypes;
        this.blockReturnTypes = blockReturnTypes;

        this.isReachable = true;
        this.isPolymorphic = false;
    }

    /**
     * Pushes a value type onto the operand stack.
     * Always pushes the concrete type, even in unreachable code.
     *
     * @param type the value type to push
     * @throws WasmAssemblerException if the value type is not supported
     */
    public void pushOperand(ValueType type) throws WasmAssemblerException {
        // Always push the concrete type, even when unreachable
        // Polymorphic behavior affects consumption, not production
        operandStack.add(type);
    }

    /**
     * Pushes multiple value types onto the operand stack.
     *
     * @param types the value types to push
     * @throws WasmAssemblerException if any value type is not supported
     */
    public void pushAllOperands(Iterable<ValueType> types) throws WasmAssemblerException {
        pushAllOperands(types.iterator());
    }

    /**
     * Pushes multiple value types onto the operand stack.
     *
     * @param types the value types to push
     * @throws WasmAssemblerException if any value type is not supported
     */
    public void pushAllOperands(Iterator<ValueType> types) throws WasmAssemblerException {
        while (types.hasNext()) {
            pushOperand(types.next());
        }
    }

    /**
     * Pops a value type from the operand stack.
     * Supports polymorphic behavior when frame is unreachable.
     *
     * @param type the value type to pop
     * @throws WasmAssemblerException if the value type does not match the expected type
     */
    public void popOperand(ValueType type) throws WasmAssemblerException {
        if (operandStack.isEmpty()) {
            if (isPolymorphic) {
                return;
            }
            throw new WasmAssemblerException("Tried to pop from empty operand stack");
        }

        ValueType popped = operandStack.remove(operandStack.size() - 1);

        if (popped instanceof UnknownType || type instanceof UnknownType) {
            return;
        }

        if (!popped.equals(type)) {
            throw new WasmAssemblerException("Expected " + type.getName() + " but got " + popped.getName());
        }
    }

    /**
     * Pops any value type from the operand stack.
     * Supports polymorphic behavior when frame is unreachable.
     *
     * @throws WasmAssemblerException if the operand stack is empty and frame is reachable
     */
    public ValueType popAnyOperand() throws WasmAssemblerException {
        if (operandStack.isEmpty()) {
            if (isPolymorphic) {
                return UnknownType.UNKNOWN;
            }

            throw new WasmAssemblerException("Tried to pop from empty operand stack");
        }

        return operandStack.remove(operandStack.size() - 1);
    }

    /**
     * Requires that the operand at the specified index matches the given type.
     *
     * @param index the index of the operand to check (0 is the top of the stack)
     * @param type the expected value type
     * @throws WasmAssemblerException if the operand does not match the expected type
     */
    public void requireOperandAt(int index, ValueType type) throws WasmAssemblerException {
        int stackTargetIndex = operandStack.size() - index - 1;

        if (stackTargetIndex < 0 || stackTargetIndex >= operandStack.size()) {
            if (isPolymorphic) {
                return;
            }

            throw new WasmAssemblerException("Tried to require operand at invalid index");
        }

        ValueType found = operandStack.get(stackTargetIndex);
        if (found instanceof UnknownType || type instanceof UnknownType) {
            return;
        }

        if (!found.equals(type)) {
            throw new WasmAssemblerException("Expected " + type.getName() + " but got " + found.getName());
        }
    }

    /**
     * Retrieve the current operand stack.
     *
     * @return the operand stack
     */
    public List<ValueType> getOperandStack() {
        return Collections.unmodifiableList(operandStack);
    }

    /**
     * Retrieves the local at the specified index.
     *
     * @param index the index of the local
     * @return the value type of the local
     * @throws WasmAssemblerException if the local index is out of bounds
     */
    public ValueType requireLocal(int index) throws WasmAssemblerException {
        if (index < 0 || index >= locals.size()) {
            throw new WasmAssemblerException("Local index out of bounds");
        }

        return locals.get(index);
    }

    /**
     * Marks the frame state as unreachable and polymorphic.
     * <p>
     * If the stack is already polymorphic, this clears all concrete types
     * from the stack.
     */
    public void markUnreachable() {
        this.operandStack.clear();
        this.isReachable = false;
        this.isPolymorphic = true;
    }

    /**
     * Marks the frame state as unreachable, but preserves
     * the current operand stack and doesn't make it polymorphic.
     */
    public void markUnreachablePreserveStack() {
        this.isReachable = false;
    }

    /**
     * Retrieve the current frame's return types.
     *
     * @return the frame's return types
     */
    public List<ValueType> getReturnTypes() {
        return returnTypes;
    }

    /**
     * Retrieve the current frame's block return types.
     *
     * @return the frame's block return types
     */
    public List<ValueType> getBlockReturnTypes() {
        return blockReturnTypes;
    }

    /**
     * Determines whether the frame state is reachable.
     *
     * @return whether the frame state is reachable
     */
    public boolean isReachable() {
        return this.isReachable;
    }

    /**
     * Moves this frame to the state where another block is being executed
     * from it.
     *
     * @param type the function type of the block
     * @return the frame inside the block
     * @throws WasmAssemblerException if the block can not be executed
     */
    public WasmFrameState beginBlock(FunctionType type) throws WasmAssemblerException {
        WasmFrameState newFrameState = new WasmFrameState(
                Collections.emptyList(),
                locals,
                returnTypes,
                Arrays.asList(type.getOutputs().asFlatArray())
        );

        List<ValueType> flatInputs = type.getInputs().asFlatList();
        if (flatInputs == null) {
            throw new WasmAssemblerException("Too many inputs for block");
        }

        for (int i = flatInputs.size() - 1; i >= 0; i--) {
            popOperand(flatInputs.get(i));
        }

        for (ValueType input : type.getInputs()) {
            newFrameState.pushOperand(input);
        }

        return newFrameState;
    }

    /**
     * Moves this frame to the state where a block finished.
     *
     * @param type the function type of the block
     * @throws WasmAssemblerException if the block can not be executed
     */
    public void endBlock(FunctionType type) throws WasmAssemblerException {
        for (ValueType output : type.getOutputs()) {
            pushOperand(output);
        }
    }

    /**
     * Branch the frame state.
     *
     * @return the branched frame state
     */
    public WasmFrameState branch() {
        WasmFrameState clone = new WasmFrameState();
        clone.operandStack.addAll(operandStack);
        clone.locals.addAll(locals);
        clone.returnTypes.addAll(returnTypes);
        clone.blockReturnTypes.addAll(blockReturnTypes);
        clone.isReachable = isReachable;
        clone.isPolymorphic = isPolymorphic;

        return clone;
    }
}
