package net.janrupf.thunderwasm.assembler;

import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.ValueType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public final class WasmFrameState {
    // Could use a java.util.Stack, but its synchronized and uses an
    // array under the hood anyway.
    private final List<ValueType> operandStack;
    private int operandSlotCount;
    private int maxOperandSlotCount;

    private final List<ValueType> locals;
    private int localSlotCount;
    private int maxLocalSlotCount;

    private boolean isReachable;

    public WasmFrameState(
            ValueType[] argumentTypes,
            List<ValueType> locals
    ) throws WasmAssemblerException {
        this.operandStack = new ArrayList<>();
        this.operandSlotCount = 0;
        this.maxOperandSlotCount = 0;

        this.locals = new ArrayList<>();
        this.locals.addAll(Arrays.asList(argumentTypes));
        this.locals.addAll(locals);

        for (ValueType type : this.locals) {
            this.localSlotCount += WasmTypeConverter.toJavaType(type).getSlotCount();
        }

        this.maxLocalSlotCount = this.localSlotCount;

        this.isReachable = true;
    }

    /**
     * Pushes a value type onto the operand stack.
     *
     * @param type the value type to push
     * @throws WasmAssemblerException if the value type is not supported
     */
    public void pushOperand(ValueType type) throws WasmAssemblerException {
        operandStack.add(type);
        operandSlotCount += WasmTypeConverter.toJavaType(type).getSlotCount();

        if (operandSlotCount > maxOperandSlotCount) {
            maxOperandSlotCount = operandSlotCount;
        }
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
     *
     * @param type the value type to pop
     * @throws WasmAssemblerException if the value type does not match the expected type
     */
    public void popOperand(ValueType type) throws WasmAssemblerException {
        if (operandStack.isEmpty()) {
            throw new WasmAssemblerException("Tried to pop from empty operand stack");
        }

        ValueType popped = operandStack.remove(operandStack.size() - 1);

        if (!popped.equals(type)) {
            throw new WasmAssemblerException("Expected " + type.getName() + " but got " + popped.getName());
        }

        operandSlotCount -= WasmTypeConverter.toJavaType(type).getSlotCount();
    }

    /**
     * Pops any value type from the operand stack.
     *
     * @throws WasmAssemblerException if the operand stack is empty
     */
    public ValueType popAnyOperand() throws WasmAssemblerException {
        if (operandStack.isEmpty()) {
            throw new WasmAssemblerException("Tried to pop from empty operand stack");
        }

        ValueType popped = operandStack.remove(operandStack.size() - 1);
        operandSlotCount -= WasmTypeConverter.toJavaType(popped).getSlotCount();

        return popped;
    }

    /**
     * Requires a value type from the operand stack.
     *
     * @param type the value type to require
     * @throws WasmAssemblerException if the operand stack is empty or the value type does not match the expected type
     */
    public void requireOperand(ValueType type) throws WasmAssemblerException {
        if (operandStack.isEmpty()) {
            throw new WasmAssemblerException("Tried to require operand from empty operand stack");
        }

        ValueType found = operandStack.get(operandStack.size() - 1);

        if (!found.equals(type)) {
            throw new WasmAssemblerException("Expected " + type.getName() + " but got " + found.getName());
        }
    }

    /**
     * Requires multiple value types from the operand stack.
     *
     * @param types the value types to require
     * @throws WasmAssemblerException if the operand stack is empty,
     *                                the value types do not match the expected types,
     *                                or there are not enough operands
     */
    public void requireOperands(LargeArray<ValueType> types) throws WasmAssemblerException {
        for (long i = 0; i < types.length(); i++) {
            ValueType requiredType = types.get(LargeArrayIndex.fromU64(i));

            long stackTargetIndex = operandStack.size() - i - 1;
            if (stackTargetIndex < 0) {
                throw new WasmAssemblerException("Tried to require more operands than available");
            }

            ValueType found = operandStack.get((int) stackTargetIndex);
            if (!found.equals(requiredType)) {
                throw new WasmAssemblerException("Expected " + requiredType.getName() + " but got " + found.getName());
            }
        }
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
     * Convert a WASM local index to a Java local index.
     *
     * @param index the WASM local index
     * @return the Java local index
     * @throws WasmAssemblerException if the local index is out of bounds or a local type is not supported
     */
    public int computeJavaLocalIndex(int index) throws WasmAssemblerException {
        if (index < 0 || index >= locals.size()) {
            throw new WasmAssemblerException("Local index out of bounds");
        }

        int javaIndex = 0;

        for (int i = 0; i < index; i++) {
            javaIndex += WasmTypeConverter.toJavaType(locals.get(i)).getSlotCount();
        }

        return javaIndex;
    }

    /**
     * Allocate a slot for a local.
     *
     * @param type the type of the local
     * @return the index of the local
     * @throws WasmAssemblerException if the local type is not supported
     */
    public int allocateLocal(ValueType type) throws WasmAssemblerException {
        int index = locals.size();
        locals.add(type);
        localSlotCount += WasmTypeConverter.toJavaType(type).getSlotCount();

        if (maxLocalSlotCount < localSlotCount) {
            maxLocalSlotCount = localSlotCount;
        }

        return index;
    }

    /**
     * Free the largest local slot.
     *
     * @throws WasmAssemblerException if the local slot is empty
     */
    public void freeLocal() throws WasmAssemblerException {
        if (locals.isEmpty()) {
            throw new WasmAssemblerException("Tried to free local from empty locals");
        }

        ValueType removed = locals.remove(locals.size() - 1);
        localSlotCount -= WasmTypeConverter.toJavaType(removed).getSlotCount();
    }

    /**
     * Marks the frame state as unreachable.
     */
    public void markUnreachable() {
        this.isReachable = false;
    }

    /**
     * Marks the frame state as reachable.
     */
    public void markReachable() {
        this.isReachable = true;
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
     * Retrieves the maximum number of operand slots that were used.
     *
     * @return the maximum number of operand slots
     */
    public int getMaxOperandSlotCount() {
        return maxOperandSlotCount;
    }

    /**
     * Retrieves the maximum number of local variables that were used.
     *
     * @return the maximum number of local variables
     */
    public int getMaxLocalSlotCount() {
        return maxLocalSlotCount;
    }

    /**
     * Computes a snapshot of the current frame state.
     *
     * @return the snapshot
     * @throws WasmAssemblerException if the snapshot computation fails
     */
    public JavaFrameSnapshot computeSnapshot() throws WasmAssemblerException {
        List<JavaType> stack = new ArrayList<>(this.operandStack.size());

        for (ValueType type : this.operandStack) {
            stack.add(WasmTypeConverter.toJavaType(type));
        }

        List<JavaType> locals = new ArrayList<>(this.locals.size());

        for (ValueType type : this.locals) {
            locals.add(WasmTypeConverter.toJavaType(type));
        }

        return new JavaFrameSnapshot(operandSlotCount, stack, localSlotCount, locals);
    }
}
