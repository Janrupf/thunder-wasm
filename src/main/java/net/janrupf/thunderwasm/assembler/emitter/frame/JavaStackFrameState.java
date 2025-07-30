package net.janrupf.thunderwasm.assembler.emitter.frame;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.types.ArrayType;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;

import java.util.*;

/**
 * Helper for tracking the state of a java stack.
 */
public final class JavaStackFrameState {
    private static final Set<PrimitiveType> INT_PRIMITIVE_TYPES = new HashSet<>();

    static {
        INT_PRIMITIVE_TYPES.add(PrimitiveType.BOOLEAN);
        INT_PRIMITIVE_TYPES.add(PrimitiveType.BYTE);
        INT_PRIMITIVE_TYPES.add(PrimitiveType.CHAR);
        INT_PRIMITIVE_TYPES.add(PrimitiveType.SHORT);
    }

    private final List<JavaLocalSlot> locals;
    private final List<JavaType> operandStack;

    // Both are counted in slots (eg. a double/long takes 2)
    private int maxLocalsSize;
    private int maxStackSize;

    private int currentStackSize;

    private int vacantMiddleSlotCount = 0;

    public JavaStackFrameState() {
        this.locals = new ArrayList<>();
        this.operandStack = new ArrayList<>();

        this.maxLocalsSize = 0;
        this.maxStackSize = 0;

        this.currentStackSize = 0;

        this.vacantMiddleSlotCount = 0;
    }

    /**
     * Allocate a local.
     *
     * @param type the type that is going to be stored in the local
     * @return the allocated local
     */
    public JavaLocal allocateLocal(JavaType type) {
        type = remapForStackFrame(type);

        JavaLocal local;
        int i;

        if (vacantMiddleSlotCount > 0) {
            for (i = 0; i < locals.size(); i++) {
                JavaLocalSlot slot = locals.get(i);

                if (slot instanceof JavaLocalSlot.Vacant) {
                    if (type.getSlotCount() > 1) {
                        // Check if the following slot is also available
                        // NOTE: The following slot always has to exist, otherwise
                        //       we wouldn't have a Vacant entry
                        if (!(locals.get(i + 1) instanceof JavaLocalSlot.Vacant)) {
                            i++; // No need to check it
                            continue;
                        }
                    }

                    local = new JavaLocal(this, i, type);
                    locals.set(i, JavaLocalSlot.used(local));

                    if (type.getSlotCount() > 1) {
                        vacantMiddleSlotCount--;
                        locals.set(i + 1, JavaLocalSlot.continuation());
                    }

                    vacantMiddleSlotCount--;
                    return local;
                }
            }
        } else {
            i = locals.size();
        }

        local = new JavaLocal(this, i, type);
        locals.add(JavaLocalSlot.used(local));

        if (type.getSlotCount() > 1) {
            locals.add(JavaLocalSlot.continuation());
        }

        if (locals.size() > maxLocalsSize) {
            this.maxLocalsSize = locals.size();
        }

        return local;
    }

    /**
     * Free an allocated local.
     *
     * @param local the local to free.
     */
    void freeLocal(JavaLocal local) {
        this.locals.set(local.getSlot(), JavaLocalSlot.vacant());
        vacantMiddleSlotCount++;

        if (local.getType().getSlotCount() > 1) {
            this.locals.set(local.getSlot() + 1, JavaLocalSlot.vacant());
            vacantMiddleSlotCount++;
        }

        // Clean up all unused slots that can be free without creating a hole
        while (!locals.isEmpty() && locals.get(locals.size() - 1) instanceof JavaLocalSlot.Vacant) {
            locals.remove(locals.size() - 1);
            vacantMiddleSlotCount--;
        }
    }

    /**
     * Check whether a local is accessible in the current frame.
     *
     * @param local the local to check
     * @throws WasmAssemblerException if the local is not accessible
     */
    public void checkLocal(JavaLocal local) throws WasmAssemblerException {
        if (!local.isValid()) {
            throw new WasmAssemblerException("Local has been invalidated");
        }

        int slotIdx = local.getSlot();
        if (slotIdx >= locals.size()) {
            throw new WasmAssemblerException("Local is out of bounds, requested slot " + slotIdx + " but only " + locals.size() + " slots are valid");
        }

        JavaLocalSlot slot = locals.get(slotIdx);
        if (slot instanceof JavaLocalSlot.Used) {
            JavaType typeInSlot = ((JavaLocalSlot.Used) slot).getLocal().getType();
            if (!typeInSlot.equals(local.getType())) {
                throw new WasmAssemblerException("Expected local of type " + local.getType() + " but got " + typeInSlot);
            }
        } else if (slot instanceof JavaLocalSlot.Continuation) {
            throw new WasmAssemblerException("Attempted to access a continuation slot directly");
        } else if (slot instanceof JavaLocalSlot.Vacant) {
            throw new WasmAssemblerException("Attempted to access a vacant slot");
        }
    }

    /**
     * Require an operand on the stack.
     *
     * @return the operand on top of the stack
     * @throws WasmAssemblerException if the stack is empty
     */
    public JavaType requireOperand() throws WasmAssemblerException {
        return requireOperand(0);
    }

    /**
     * Require an operand on the stack at the given depth.
     *
     * @param depth the number of operands from the top
     * @return the operand on top of the stack
     * @throws WasmAssemblerException if no operand exists at that position
     */
    public JavaType requireOperand(int depth) throws WasmAssemblerException {
        if (operandStack.isEmpty()) {
            throw new WasmAssemblerException("Operand stack is empty");
        } else if (depth >= operandStack.size()) {
            throw new WasmAssemblerException("Required operand at depth " + depth + ", but stack has only " + operandStack.size() + " operands");
        }

        return operandStack.get(operandStack.size() - 1 - depth);
    }

    /**
     * Count the amount of operands currently on the stack.
     *
     * @return the amount of operands on the stack
     */
    public int operandStackCount() {
        return operandStack.size();
    }

    /**
     * Push a new operand on to the stack.
     *
     * @param type the type of the operand that is being pushed
     */
    public void pushOperand(JavaType type) {
        type = remapForStackFrame(type);

        if (type.equals(PrimitiveType.VOID)) {
            throw new IllegalArgumentException("void can't be pushed onto the operand stack");
        }

        operandStack.add(type);

        currentStackSize += type.getSlotCount();
        if (currentStackSize > maxStackSize) {
            maxStackSize = currentStackSize;
        }
    }

    /**
     * Push multiple operands on the stack.
     *
     * @param types the types of the operands to push
     */
    public void pushOperands(JavaType... types) {
        for (JavaType type : types) {
            pushOperand(type);
        }
    }

    /**
     * Pop an operand from the stack.
     *
     * @return the popped operand
     * @throws WasmAssemblerException if the operand stack is empty
     */
    public JavaType popAnyOperand() throws WasmAssemblerException {
        if (operandStack.isEmpty()) {
            throw new WasmAssemblerException("Attempted to pop from an empty operand stack");
        }

        return operandStack.remove(operandStack.size() - 1);
    }

    /**
     * Pop an operand from the stack if the stack is not empty.
     *
     * @return the popped operand, or null, if the stack was empty
     */
    public JavaType popAnyOperandIfNotEmpty() {
        if (operandStack.isEmpty()) {
            return null;
        }

        return operandStack.remove(operandStack.size() - 1);
    }

    /**
     * Pop an operand from the stack.
     * <p>
     * This method does check that the operand types are compatible, however, since
     * {@link JavaType} represents a potentially not loaded reference type,
     * no inheritance checking is done. In other words, all reference types
     * are considered compatible, regardless of whether their (hypothetical) class
     * structure matches. Also note that all types smaller than int are automatically
     * upcasted to int.
     *
     * @param type the type of the operand to pop
     * @return the popped operand type
     * @throws WasmAssemblerException if an operand type mismatch is detected or the stack is empty
     */
    public JavaType popOperand(JavaType type) throws WasmAssemblerException {
        type = remapForStackFrame(type);

        JavaType removed = popAnyOperand();

        if ((!removed.equals(type) && !(type instanceof ObjectType && removed instanceof ObjectType)) ||
                (type instanceof ArrayType && !(removed instanceof ArrayType))) {
            throw new WasmAssemblerException("Operand type mismatch detected, expected " + type + " but found " + removed);
        }

        return removed;
    }

    /**
     * Pop multiple operands from the stack.
     * <p>
     * See {@link #popOperand(JavaType)} for type checking details.
     *
     * @param types the types to pop from the stack
     * @throws WasmAssemblerException if an operand type mismatch is detected or the stack becomes empty before popping all
     */
    public void popOperands(JavaType... types) throws WasmAssemblerException {
        for (JavaType type : types) {
            popOperand(type);
        }
    }

    /**
     * Retrieve the currently computed maximum stack size (in slots).
     *
     * @return the maximum stack size
     */
    public int maxStackSize() {
        return maxStackSize;
    }

    /**
     * Retrieve the currently computed maximum local size (in slots).
     *
     * @return the maximum local size
     */
    public int maxLocalsSize() {
        return maxLocalsSize;
    }

    /**
     * Compute a snapshot of the current state.
     *
     * @return the computed snapshot
     */
    public JavaFrameSnapshot computeSnapshot() {
        List<JavaType> operands = new ArrayList<>(this.operandStack);
        List<JavaLocalSlot> locals = new ArrayList<>(this.locals);

        return new JavaFrameSnapshot(
                operands,
                locals
        );
    }

    /**
     * Restore the state from a snapshot.
     */
    public void restoreFromSnapshot(JavaFrameSnapshot snapshot) {
        this.operandStack.clear();
        this.locals.clear();
        this.currentStackSize = 0;

        // TODO: This doesn't work nicely with local invalidation

        this.locals.addAll(snapshot.getLocals());
        if (this.maxLocalsSize < this.locals.size()) {
            this.maxLocalsSize = this.locals.size();
        }

        for (JavaType operand : snapshot.getStack()) {
            this.pushOperand(operand);
        }
    }

    public JavaType remapForStackFrame(JavaType type) {
        if (type instanceof PrimitiveType && INT_PRIMITIVE_TYPES.contains(type)) {
            return PrimitiveType.INT;
        }

        return type;
    }
}
