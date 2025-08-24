package net.janrupf.thunderwasm.eval;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.instructions.Expr;
import net.janrupf.thunderwasm.instructions.InstructionInstance;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.lookup.ElementLookups;
import net.janrupf.thunderwasm.types.ValueType;

/**
 * Evaluation context for the WebAssembly interpreter.
 * <p>
 * Currently, this is only used for const expressions.
 */
public final class EvalContext {
    private final EvalFrameState frameState;
    private final ElementLookups lookups;
    private final boolean allowReferencingNonImportGlobals;

    public EvalContext(
            ElementLookups lookups,
            boolean allowReferencingNonImportGlobals
    ) {
        this.frameState = new EvalFrameState();
        this.lookups = lookups;
        this.allowReferencingNonImportGlobals = allowReferencingNonImportGlobals;
    }

    /**
     * Retrieves the current evaluation frame state.
     *
     * @return the frame state
     */
    public EvalFrameState getFrameState() {
        return frameState;
    }

    /**
     * Retrieves the lookups that are used to look up elements.
     *
     * @return the lookups
     */
    public ElementLookups getLookups() {
        return lookups;
    }

    /**
     * Evaluates the given expression.
     *
     * @param expr         the expression to evaluate
     * @param requireConst whether the expression must be constant
     * @throws WasmAssemblerException if the evaluation fails
     */
    public void eval(Expr expr, boolean requireConst) throws WasmAssemblerException {
        for (InstructionInstance instructionInstance : expr.getInstructions()) {
            WasmInstruction<?> instruction = instructionInstance.getInstruction();
            WasmInstruction.Data data = instructionInstance.getData();

            if (requireConst && !instruction.isConst()) {
                throw new WasmAssemblerException("Found non-const instruction in const expression: " + instruction.getName());
            }

            evalInstruction(instruction, data);
        }
    }

    /**
     * Evaluates the given expression and requires a single value of the given type.
     *
     * @param expr         the expression to evaluate
     * @param requireConst whether the expression must be constant
     * @param type         the type of the value
     * @return the value
     * @throws WasmAssemblerException if the evaluation fails
     */
    public Object evalSingleValue(Expr expr, boolean requireConst, ValueType type) throws WasmAssemblerException {
        eval(expr, requireConst);
        return requireSingleValue(type);
    }

    @SuppressWarnings("unchecked")
    private <T extends WasmInstruction.Data> void evalInstruction(
            WasmInstruction<T> instruction,
            Object data
    ) throws WasmAssemblerException {
        instruction.eval(this, (T) data);
    }

    /**
     * Requires a single value of the given type from the stack.
     *
     * @param type the type of the value
     * @return the value
     * @throws WasmAssemblerException if the value is not found or has the wrong type
     */
    public Object requireSingleValue(ValueType type) throws WasmAssemblerException {
        if (frameState.getStackSize() != 1) {
            throw new WasmAssemblerException("Expected a single value on the stack, but found " + frameState.getStackSize());
        }

        StackEntry entry = frameState.pop();
        if (!entry.getType().equals(type)) {
            throw new WasmAssemblerException("Expected a value of type " + type + " but found " + entry.getType());
        }

        return entry.getValue();
    }

    /**
     * Derives a fresh evaluation context from this context.
     *
     * @param allowReferencingNonImportGlobals whether the new context should allow referencing non-import globals
     * @return the fresh evaluation context
     */
    public EvalContext deriveFresh(boolean allowReferencingNonImportGlobals) {
        return new EvalContext(lookups, allowReferencingNonImportGlobals);
    }

    /**
     * Checks whether referencing non-import globals is allowed.
     *
     * @return true if allowed, false otherwise
     */
    public boolean doesAllowReferencingNonImportGlobals() {
        return allowReferencingNonImportGlobals;
    }
}
