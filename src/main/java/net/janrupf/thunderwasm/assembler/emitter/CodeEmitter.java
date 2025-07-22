package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.JavaFrameSnapshot;
import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaStackFrameState;
import net.janrupf.thunderwasm.assembler.emitter.types.ArrayType;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;

public interface CodeEmitter {
    /**
     * Retrieve the type of the class being emitted.
     *
     * @return the type of the class being emitted
     */
    ObjectType getOwner();

    /**
     * Retrieve the state the stack frame is in.
     *
     * @return the stack frame state, or null, if unknown
     */
    JavaStackFrameState getStackFrameState();

    /**
     * Fix up a frame which was inferred from WASM state.
     * <p>
     * The way the code emitter organizes locals is not necessarily
     * the same that WASM does it. For example, WASM doesn't have a
     * concept of a "this" local. Moreover, the emitter may decide
     * to re-order locals.
     * <p>
     * There are cases where frame state needs to be inferred from
     * WASM state (mainly when dealing with WASM code that is unreachable).
     * This inferred frame state can not accommodate for emitter specific
     * intricacies.
     *
     * @param snapshot the snapshot to fix up
     * @return the fixed up snapshot with emitter intricacies applied
     * @throws WasmAssemblerException if the emitter can not fix up the frame
     */
    JavaFrameSnapshot fixupInferredFrame(JavaFrameSnapshot snapshot) throws WasmAssemblerException;

    /**
     * Creates a new, not yet resolved label.
     *
     * @return the new label
     */
    CodeLabel newLabel();

    /**
     * Resolves the label to the current position in the code.
     * <p>
     * This automatically attaches the current frame state to the lable.
     *
     * @param label the label to resolve
     * @throws WasmAssemblerException if the label is already resolved
     */
    default void resolveLabel(CodeLabel label) throws WasmAssemblerException {
        resolveLabel(label, null);
    }

    /**
     * Resolves the label to the current position in the code.
     *
     * @param label the label to resolve
     * @param frame the frame to attach to the label, or null, if no frame should be attached
     * @throws WasmAssemblerException if the label is already resolved
     */
    void resolveLabel(CodeLabel label, JavaFrameSnapshot frame) throws WasmAssemblerException;

    /**
     * Emit an instruction loading a constant onto the stack.
     * <p>
     * This automatically chooses the correct instruction based on the type and constant value.
     *
     * @param value the value to load
     * @throws WasmAssemblerException if the constant type is not supported
     */
    void loadConstant(Object value) throws WasmAssemblerException;

    /**
     * Emit a return instruction.
     *
     * @throws WasmAssemblerException if a return can not be generated
     */
    void doReturn() throws WasmAssemblerException;

    /**
     * Emit a jump instruction.
     *
     * @param condition the condition for the jump
     * @param target    the target label
     * @throws WasmAssemblerException if the jump instruction is invalid
     */
    void jump(JumpCondition condition, CodeLabel target) throws WasmAssemblerException;

    /**
     * Load the "this" reference onto the stack.
     *
     * @throws WasmAssemblerException if the code has no "this" reference
     */
    void loadThis() throws WasmAssemblerException;

    /**
     * Retrieve the local for a specific argument.
     *
     * @param index the index of the argument
     * @return the local for that argument
     * @throws WasmAssemblerException if the argument doesn't exist
     */
    JavaLocal getArgumentLocal(int index) throws WasmAssemblerException;

    /**
     * Allocate a temporary local.
     *
     * @param type the type stored in the local
     * @return the allocated local
     * @throws WasmAssemblerException if allocation fails
     */
    JavaLocal allocateLocal(JavaType type) throws WasmAssemblerException;

    /**
     * Load a local variable onto the stack.
     * <p>
     * This method automatically adjusts for "this" locals.
     *
     * @param local the local to load
     * @throws WasmAssemblerException if the local variable cannot be loaded
     */
    void loadLocal(JavaLocal local) throws WasmAssemblerException;

    /**
     * Store a value from the stack into a local variable.
     * <p>
     * This method automatically adjusts for "this" locals.
     *
     * @param local the local to load
     * @throws WasmAssemblerException if the local variable cannot be stored
     */
    void storeLocal(JavaLocal local) throws WasmAssemblerException;

    /**
     * Emit an invoke instruction.
     *
     * @param type             the type of the object to invoke the method on
     * @param methodName       the name of the method to invoke
     * @param parameterTypes   the types of the parameters
     * @param returnType       the return type of the method
     * @param invokeType       the type of the invocation
     * @param ownerIsInterface whether the owner of the method is an interface
     * @throws WasmAssemblerException if the invoke instruction is invalid
     */
    void invoke(
            JavaType type,
            String methodName,
            JavaType[] parameterTypes,
            JavaType returnType,
            InvokeType invokeType,
            boolean ownerIsInterface
    ) throws WasmAssemblerException;

    /**
     * Emit a new or anewarray instruction.
     *
     * @param type the type of the object to create
     * @throws WasmAssemblerException if the new instruction is invalid
     */
    void doNew(ObjectType type) throws WasmAssemblerException;

    /**
     * Emit a field access instruction.
     *
     * @param type      the type of the object to access the field on
     * @param fieldName the name of the field to access
     * @param fieldType the type of the field
     * @param isStatic  whether the field is static
     * @param isSet     whether the field is being set
     * @throws WasmAssemblerException if the field access instruction is invalid
     */
    void accessField(
            JavaType type,
            String fieldName,
            JavaType fieldType,
            boolean isStatic,
            boolean isSet
    ) throws WasmAssemblerException;

    /**
     * Emit a dup/dup2 instruction.
     *
     * @throws WasmAssemblerException if the duplicate instruction is invalid
     */
    default void duplicate() throws WasmAssemblerException {
        duplicate(1, 0);
    }

    /**
     * Emit an instruction of the dup family.
     * <p>This method duplicates the top {@code count} values on the stack and inserts
     * them at a specified {@code depth}.
     *
     * <p>For example, with a stack of {@code [..., value3, value2, value1]}, where
     * {@code value1} is at the top:
     * <ul>
     *   <li>{@code duplicate(1, 0)} (dup): results in {@code [..., value3, value2, value1, value1]}</li>
     *   <li>{@code duplicate(1, 1)} (dup_x1): results in {@code [..., value3, value1, value2, value1]}</li>
     *   <li>{@code duplicate(1, 2)} (dup_x2): results in {@code [..., value1, value3, value2, value1]}</li>
     * </ul>
     *
     * @param count how many elements to duplicate (1 or 2)
     * @param depth how many elements to move down before inserting (0 - 2)
     * @throws WasmAssemblerException if the duplicate instruction is invalid
     */
    void duplicate(int count, int depth) throws WasmAssemblerException;

    /**
     * Emit a pop/pop2 instruction.
     *
     * @throws WasmAssemblerException if the value cannot be popped
     */
    void pop() throws WasmAssemblerException;

    /**
     * Emit an a<type>store instruction.
     *
     * @throws WasmAssemblerException if the array element cannot be stored
     */
    void storeArrayElement() throws WasmAssemblerException;

    /**
     * Emit an a<type>load instruction.
     *
     * @throws WasmAssemblerException if the array element cannot be loaded
     */
    void loadArrayElement() throws WasmAssemblerException;

    /**
     * Emit a simple operation.
     *
     * @throws WasmAssemblerException if the operation is not valid
     */
    void op(Op op) throws WasmAssemblerException;

    /**
     * Emit a cast check instruction.
     *
     * @param type the type to check for
     * @throws WasmAssemblerException if the cast check instruction is invalid
     */
    void checkCast(ObjectType type) throws WasmAssemblerException;

    /**
     * Finish the code generation.
     *
     * @throws WasmAssemblerException if the code generation fails
     */
    void finish() throws WasmAssemblerException;
}
