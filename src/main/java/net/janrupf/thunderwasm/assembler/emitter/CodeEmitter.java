package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.JavaFrameSnapshot;
import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
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
     * Creates a new, not yet resolved label.
     *
     * @return the new label
     */
    CodeLabel newLabel();

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
     * @param type the return type
     * @throws WasmAssemblerException if the return type is not supported
     */
    void doReturn(JavaType type) throws WasmAssemblerException;

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
     * Load a local variable onto the stack.
     * <p>
     * This method automatically adjusts for "this" locals.
     *
     * @param index the index of the local variable
     * @param type  the type of the local variable
     * @throws WasmAssemblerException if the local variable cannot be loaded
     */
    void loadLocal(int index, JavaType type) throws WasmAssemblerException;

    /**
     * Store a value from the stack into a local variable.
     * <p>
     * This method automatically adjusts for "this" locals.
     *
     * @param index the index of the local variable
     * @param type  the type of the local variable
     * @throws WasmAssemblerException if the local variable cannot be stored
     */
    void storeLocal(int index, JavaType type) throws WasmAssemblerException;

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
     * Emit a dup instruction.
     *
     * @param type the type of the value to duplicate
     * @throws WasmAssemblerException if the value cannot be duplicated
     */
    void duplicate(JavaType type) throws WasmAssemblerException;

    /**
     * Emit a dup2 instruction.
     *
     * @param first  the type of the first value to duplicate
     * @param second the type of the second value to duplicate
     * @throws WasmAssemblerException if the value cannot be duplicated
     */
    void duplicate2(JavaType first, JavaType second) throws WasmAssemblerException;

    /**
     * Emit a dup_x2 instruction.
     *
     * @param type the type of the value to duplicate
     * @throws WasmAssemblerException if the value cannot be duplicated
     */
    void duplicateX2(JavaType type) throws WasmAssemblerException;

    /**
     * Emit a pop instruction.
     *
     * @param type the type of the value to pop
     * @throws WasmAssemblerException if the value cannot be popped
     */
    void pop(JavaType type) throws WasmAssemblerException;

    /**
     * Emit a simple operation.
     *
     * @throws WasmAssemblerException if the operation is not valid
     */
    void op(Op op) throws WasmAssemblerException;

    /**
     * Finish the code generation.
     * <p>
     * This method automatically adjusts for "this" locals.
     *
     * @param maxOperands the maximum number of operands on the stack
     * @param maxLocals   the maximum number of local variables
     * @throws WasmAssemblerException if the code generation fails
     */
    void finish(int maxOperands, int maxLocals) throws WasmAssemblerException;
}
