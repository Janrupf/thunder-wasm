package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.emitter.frame.JavaFrameSnapshot;
import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaStackFrameState;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;

import java.util.Map;

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
     * Create a new code emitter that can be used to generate gadgets.
     * <p>
     * The emitter is bound to the method that this emitter is bound to.
     *
     * @return the new emitter
     */
    CodeEmitter codeGadget(JavaFrameSnapshot initialState);

    /**
     * Insert the code of another emitter at the beginning.
     * <p>
     * The emitter that supplies the instructions for prepending
     * is effectively cleared and should not be used again.
     *
     * @param emitter the emitter to insert the code from
     * @throws WasmAssemblerException if the code can not be prepended
     */
    void prepend(CodeEmitter emitter) throws WasmAssemblerException;

    /**
     * Insert the code of another emitter at the end.
     * <p>
     * The emitter that supplies the instructions for prepending
     * is effectively cleared and should not be used again.
     *
     * @param emitter the emitter to insert the code from
     * @throws WasmAssemblerException if the code cannot be appended
     */
    void append(CodeEmitter emitter) throws WasmAssemblerException;

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
     * Emit a tableswitch instruction.
     *
     * @param base the base value (lower bound, aka min)
     * @param defaultLabel the value to jump to if no appropriate label case is found
     * @param targets the target labels
     * @throws WasmAssemblerException if the tableswitch instruction is invalid
     */
    void tableSwitch(int base, CodeLabel defaultLabel, CodeLabel... targets) throws WasmAssemblerException;

    /**
     * Emit a lookup switch instruction.
     * <p>
     * The emitter is free to compile this to a tableswitch if the keys allow it.
     *
     * @param targets the target labels, indexed by the value to match
     * @param defaultLabel the value to jump to if no appropriate label case is found
     * @throws WasmAssemblerException if the lookup switch instruction is invalid
     */
    void lookupSwitch(CodeLabel defaultLabel, Map<Integer, CodeLabel> targets) throws WasmAssemblerException;

    /**
     * Finish the code generation.
     *
     * @throws WasmAssemblerException if the code generation fails
     */
    void finish() throws WasmAssemblerException;
}
