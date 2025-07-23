package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.data.Limits;
import net.janrupf.thunderwasm.runtime.ExternReference;
import net.janrupf.thunderwasm.runtime.FunctionReference;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;
import net.janrupf.thunderwasm.types.VecType;

public class CommonBytecodeGenerator {
    private CommonBytecodeGenerator() {
    }

    /**
     * Convert the top i32 from unsigned to signed, swapping them in the process.
     *
     * @param emitter    the code emitter
     * @throws WasmAssemblerException if the conversion is invalid
     */
    public static void swapConvertUnsignedI32(
            CodeEmitter emitter
    ) throws WasmAssemblerException {
        // Notify the state that we will push and pop an additional i32

        // We need to add Integer.MIN_VALUE to the operands to convert them to unsigned integers
        // which can be compared using the signed comparison instruction
        emitter.loadConstant(Integer.MIN_VALUE);
        emitter.op(Op.IADD);

        // Now that we converted one operand to an unsigned integer, swap the operands
        emitter.op(Op.SWAP);

        // and convert the other operand to an unsigned integer
        emitter.loadConstant(Integer.MIN_VALUE);
        emitter.op(Op.IADD);
    }

    /**
     * Convert the top 2 values from unsigned to signed.
     *
     * @param emitter    the code emitter
     * @param type       the type of the values to convert
     * @throws WasmAssemblerException if the conversion is invalid
     */
    public static void convertTop2Unsigned(
            CodeEmitter emitter,
            NumberType type
    ) throws WasmAssemblerException {
        Op addOp;

        if (type.equals(NumberType.I64)) {
            addOp = Op.LADD;
        } else {
            throw new WasmAssemblerException("Unsupported type for unsigned conversion: " + type);
        }

        JavaLocal local = emitter.allocateLocal(WasmTypeConverter.toJavaType(type));

        // Convert the value on top
        emitter.loadConstant(type.getMinValue());
        emitter.op(addOp);

        // Store the converted value in a local
        emitter.storeLocal(local);

        // Now convert the value which was below
        emitter.loadConstant(type.getMinValue());
        emitter.op(addOp);

        // Load back the local
        emitter.loadLocal(local);


        local.free();
    }

    /**
     * Helper function to generate a conditional jump that pushes 0 or 1 on the stack.
     *
     * @param emitter    the code emitter
     * @param condition  the condition to evaluate
     * @throws WasmAssemblerException if the condition is invalid
     */
    public static void evalConditionZeroOrOne(
            CodeEmitter emitter,
            JumpCondition condition
    ) throws WasmAssemblerException {
        CodeLabel successLabel = emitter.newLabel();
        CodeLabel finishedLabel = emitter.newLabel();

        // Jump to success label if the condition is met
        emitter.jump(condition, successLabel);

        // Fallthrough: condition is not met
        emitter.loadConstant(0);
        emitter.jump(JumpCondition.ALWAYS, finishedLabel);

        // Success: condition is met
        emitter.resolveLabel(successLabel);
        emitter.loadConstant(1);

        // Finished
        emitter.resolveLabel(finishedLabel);
    }

    /**
     * Helper function to evaluate a comparison result and push 0 or 1 on the stack
     * depending on whether the result matches the target result.
     *
     * @param emitter      the code emitter
     * @param targetResult the target result to compare against
     * @throws WasmAssemblerException if the comparison cannot be generated
     */
    public static void evalCompResultZeroOrOne(
            CodeEmitter emitter,
            ComparisonResult targetResult
    ) throws WasmAssemblerException {
        switch (targetResult) {
            case LESS_THAN:

                // Shift amount to the right by 31 bits, this only leaves -1 to be 1
                emitter.loadConstant(31);
                emitter.op(Op.IUSHR);

                break;
            case GREATER_THAN:

                // Add 1 to get 1 or 2
                emitter.loadConstant(1);
                emitter.op(Op.IADD);

                // Shift the value to the right by 1 bit
                emitter.loadConstant(1);
                emitter.op(Op.IUSHR);

                break;
            case EQUAL:

                // Xor the value with -1 to invert all bits
                emitter.loadConstant(-1);
                emitter.op(Op.IXOR);

                // And with 1 to get 1 if the value was 0
                emitter.loadConstant(1);
                emitter.op(Op.IAND);

                break;
            case LESS_THAN_OR_EQUAL:

                // Subtract 1 from the value
                emitter.loadConstant(1);
                emitter.op(Op.ISUB);

                // Shift amount to the right by 31 bits, this only leaves -1 or -2 to be 1
                emitter.loadConstant(31);
                emitter.op(Op.IUSHR);

                break;
            case GREATER_THAN_OR_EQUAL:

                // Add 2 to get 2 or 3
                emitter.loadConstant(2);
                emitter.op(Op.IADD);

                // Shift the value to the right by 1 bit
                emitter.loadConstant(1);
                emitter.op(Op.IUSHR);

                break;
            case NOT_EQUAL:

                // And with 1 to get 1 if the value was -1 or 1
                emitter.loadConstant(1);
                emitter.op(Op.IAND);

                break;
        }
    }

    /**
     * Evaluate if any of 2 floats on top of the stack is NaN.
     *
     * @param emitter    the code emitter
     * @param target     the target result to generate
     * @throws WasmAssemblerException if the evaluation cannot be generated
     */
    public static void eval2FloatsNaN(
            CodeEmitter emitter,
            NaNTargetResult target
    ) throws WasmAssemblerException {
        Op compareOp;

        switch (target) {
            case ONE:
                compareOp = Op.FCMPG;
                break;

            case ZERO:
            case MINUS_ONE:
                compareOp = Op.FCMPL;
                break;

            default:
                throw new WasmAssemblerException("Unsupported NaN target result: " + target);
        }


        // Get copies of both floats to check for NaN
        emitter.duplicate(2, 0);

        // Check the first float for NaN by duplicating it and comparing it to itself
        emitter.duplicate();
        emitter.op(compareOp);

        // Get the second float on top and compare it to itself
        emitter.op(Op.SWAP);
        emitter.duplicate();
        emitter.op(compareOp);

        // Combine both results to check if either of the floats is NaN
        emitter.op(Op.IOR);

        // Invert -1 to 0 and 0 to -1
        if (target == NaNTargetResult.ZERO) {
            emitter.loadConstant(-1);
            emitter.op(Op.IXOR);
        }


    }

    /**
     * Generate a float comparison that always returns 0 if NaN is detected.
     *
     * @param emitter      the code emitter
     * @param targetResult the target result to generate
     * @throws WasmAssemblerException if the evaluation cannot be generated
     */
    public static void evalFloatCompNaNZero(
            CodeEmitter emitter,
            ComparisonResult targetResult
    ) throws WasmAssemblerException {
        // Check for NaN, pushing 0 if NaN is detected, -1 otherwise
        CommonBytecodeGenerator.eval2FloatsNaN(emitter, NaNTargetResult.ZERO);

        // Move the NaN check result 2 values down and then discard the top copy
        emitter.duplicate(1, 2);
        emitter.pop();

        emitter.op(Op.FCMPG);

        CommonBytecodeGenerator.evalCompResultZeroOrOne(emitter, targetResult);

        // AND the two results
        emitter.op(Op.IAND);
    }

    /**
     * Evaluate if any of 2 doubles on top of the stack is NaN.
     *
     * @param emitter    the code emitter
     * @param target     the target result to generate
     * @throws WasmAssemblerException if the evaluation cannot be generated
     */
    public static void eval2DoublesNaN(
            CodeEmitter emitter,
            NaNTargetResult target
    ) throws WasmAssemblerException {
        Op compareOp;

        switch (target) {
            case ONE:
                compareOp = Op.DCMPG;
                break;

            case ZERO:
            case MINUS_ONE:
                compareOp = Op.DCMPL;
                break;

            default:
                throw new WasmAssemblerException("Unsupported NaN target result: " + target);
        }


        JavaLocal firstLocal = emitter.allocateLocal(PrimitiveType.DOUBLE);
        JavaLocal secondLocal = emitter.allocateLocal(PrimitiveType.DOUBLE);
        JavaLocal nanStateLocal = emitter.allocateLocal(PrimitiveType.INT);

        // Store the original doubles into locals
        emitter.storeLocal(secondLocal);
        emitter.storeLocal(firstLocal);

        // Load the second double and check it for NaN
        emitter.loadLocal(secondLocal);
        emitter.duplicate();

        // Check for NaN and store the result
        emitter.op(compareOp);
        emitter.storeLocal(nanStateLocal);

        // Load the first double and check it for NaN
        emitter.loadLocal(firstLocal);
        emitter.duplicate();

        // Check for NaN
        emitter.op(compareOp);

        // Load the previous result
        emitter.loadLocal(nanStateLocal);

        // Combine both results to check if either of the doubles is NaN
        emitter.op(Op.IOR);

        // Invert -1 to 0 and 0 to -1
        if (target == NaNTargetResult.ZERO) {
            emitter.loadConstant(-1);
            emitter.op(Op.IXOR);
        }

        // Get back the doubles
        emitter.loadLocal(firstLocal);
        emitter.loadLocal(secondLocal);

        // Free the locals
        firstLocal.free();
        secondLocal.free();
        nanStateLocal.free();
    }

    /**
     * Generate a double comparison that always returns 0 if NaN is detected.
     *
     * @param emitter      the code emitter
     * @param targetResult the target result to generate
     * @throws WasmAssemblerException if the evaluation cannot be generated
     */
    public static void evalDoubleCompNaNZero(
            CodeEmitter emitter,
            ComparisonResult targetResult
    ) throws WasmAssemblerException {
        // Check for NaN, pushing 0 if NaN is detected, -1 otherwise
        CommonBytecodeGenerator.eval2DoublesNaN(emitter, NaNTargetResult.ZERO);

        emitter.op(Op.DCMPG);

        CommonBytecodeGenerator.evalCompResultZeroOrOne(emitter, targetResult);

        // AND the two results
        emitter.op(Op.IAND);
    }

    /**
     * Swap the top two values on the stack.
     *
     * @param emitter    the code emitter
     * @throws WasmAssemblerException if the swap cannot be generated
     */
    public static void swap(
            CodeEmitter emitter
    ) throws WasmAssemblerException {
        JavaType topType = emitter.getStackFrameState().requireOperand(0);
        JavaType belowType = emitter.getStackFrameState().requireOperand(1);

        if (topType.getSlotCount() < 2 && belowType.getSlotCount() < 2) {
            // Simple swap
            emitter.op(Op.SWAP);
        } else {
            // Swap using locals
            JavaLocal topLocal = emitter.allocateLocal(topType);
            JavaLocal belowLocal = emitter.allocateLocal(belowType);

            emitter.storeLocal(topLocal);
            emitter.storeLocal(belowLocal);

            emitter.loadLocal(topLocal);
            emitter.loadLocal(belowLocal);

            topLocal.free();
            belowLocal.free();
        }
    }

    /**
     * Load the value of "this" and move it below n values.
     *
     * @param emitter         the code emitter
     * @param localVariables  the local variables
     * @param valuesAboveThis the number of values above "this"
     * @throws WasmAssemblerException if the load cannot be generated
     */
    public static void loadThisBelow(
            CodeEmitter emitter,
            LocalVariables localVariables,
            int valuesAboveThis
    ) throws WasmAssemblerException {
        loadBelow(emitter, valuesAboveThis, emitter.getOwner(), () -> emitter.loadLocal(localVariables.getThis()));
    }

    /**
     * Load a value and move it below n values.
     *
     * @param emitter         the code emitter
     * @param valuesAbove     the number of values above the value
     * @param type            the type of the value
     * @param emitterFunction the function to invoke to generate the emitting instructions,
     *                        the emitter should not manipulate the frame state
     * @throws WasmAssemblerException if the load cannot be generated
     */
    public static void loadBelow(
            CodeEmitter emitter,
            int valuesAbove,
            JavaType type,
            Emitter emitterFunction
    ) throws WasmAssemblerException {
        if (valuesAbove == 0) {
            emitterFunction.emit();
            return;
        }

        // Remove all values above
        JavaType[] toPop = new JavaType[valuesAbove];
        for (int i = 0; i < valuesAbove; i++) {
            toPop[i] = emitter.getStackFrameState().requireOperand(valuesAbove - 1 - i);
        }

        if (toPop.length == 1 && toPop[0].getSlotCount() < 2 && type.getSlotCount() < 2) {
            // Simple swap
            emitterFunction.emit();
            emitter.op(Op.SWAP);
        } else if (
                toPop.length == 2 &&
                        toPop[0].getSlotCount() < 2 &&
                        toPop[1].getSlotCount() < 2 &&
                        type.getSlotCount() < 2
        ) {
            // Load now
            emitterFunction.emit();

            // Use a duplicate down operation
            emitter.duplicate(1, 2);

            // And drop the value on top
            emitter.pop();
        } else {
            // Transfer the values to locals
            JavaLocal[] locals = new JavaLocal[toPop.length];
            for (int i = 0; i < toPop.length; i++) {
                locals[i] = emitter.allocateLocal(toPop[i]);
                emitter.storeLocal(locals[i]);
            }

            // Load value and push it
            emitterFunction.emit();

            // Load the values back
            for (int i = toPop.length - 1; i >= 0; i--) {
                emitter.loadLocal(locals[i]);
                locals[i].free();
            }
        }
    }

    /**
     * Load the value type reference.
     *
     * @param emitter    the code emitter
     * @param type       the value type
     * @throws WasmAssemblerException if the load cannot be generated
     */
    public static void loadTypeReference(
            CodeEmitter emitter,
            ValueType type
    ) throws WasmAssemblerException {
        ObjectType fieldOwner = ObjectType.of(type.getClass());
        String fieldName;

        if (type.equals(NumberType.I32)) {
            fieldName = "I32";
        } else if (type.equals(NumberType.I64)) {
            fieldName = "I64";
        } else if (type.equals(NumberType.F32)) {
            fieldName = "F32";
        } else if (type.equals(NumberType.F64)) {
            fieldName = "F64";
        } else if (type.equals(ReferenceType.EXTERNREF)) {
            fieldName = "EXTERNREF";
        } else if (type.equals(ReferenceType.FUNCREF)) {
            fieldName = "FUNCREF";
        } else if (type.equals(VecType.V128)) {
            fieldName = "V128";
        } else {
            throw new WasmAssemblerException("Unsupported type: " + type);
        }

        emitter.accessField(
                fieldOwner,
                fieldName,
                fieldOwner,
                true,
                false
        );
    }

    /**
     * Load the limits.
     *
     * @param emitter    the code emitter
     * @param limits     the limits to load
     * @throws WasmAssemblerException if the load cannot be generated
     */
    public static void loadLimits(CodeEmitter emitter, Limits limits)
            throws WasmAssemblerException {

        // Construct the limits instance
        emitter.doNew(ObjectType.of(Limits.class));
        emitter.duplicate();

        // Push min (int) and max (Integer)
        emitter.loadConstant(limits.getMin());

        if (limits.getMax() == null) {
            emitter.loadConstant(null);
        } else {
            // Need to box!
            emitter.loadConstant(limits.getMax());
            emitter.invoke(
                    ObjectType.of(Integer.class),
                    "valueOf",
                    new JavaType[]{PrimitiveType.INT},
                    ObjectType.of(Integer.class),
                    InvokeType.STATIC,
                    false
            );

        }

        emitter.invoke(
                ObjectType.of(Limits.class),
                "<init>",
                new JavaType[]{PrimitiveType.INT, ObjectType.of(Integer.class)},
                PrimitiveType.VOID,
                InvokeType.SPECIAL,
                false
        );

    }

    /**
     * Load a constant value.
     * <p>
     * This is mostly equivalent to {@link CodeEmitter#loadConstant(Object)} but also supports
     * {@link net.janrupf.thunderwasm.runtime.FunctionReference} and
     * {@link net.janrupf.thunderwasm.runtime.ExternReference} values.
     *
     * @param emitter    the code emitter
     * @param type       the type of the value
     * @param value      the value to load
     * @throws WasmAssemblerException if the constant type is not supported
     */
    public static void loadConstant(
            CodeEmitter emitter,
            ValueType type,
            Object value
    ) throws WasmAssemblerException {
        if (value instanceof FunctionReference) {
            if (!type.equals(ReferenceType.FUNCREF)) {
                throw new WasmAssemblerException("Invalid type for function reference: " + type);
            }

            ObjectType functionReferenceType = ObjectType.of(FunctionReference.class);

            emitter.loadConstant(((FunctionReference) value).getFunctionIndex());

            emitter.invoke(
                    functionReferenceType,
                    "of",
                    new JavaType[]{PrimitiveType.INT},
                    functionReferenceType,
                    InvokeType.STATIC,
                    false
            );

            return;
        } else if (value instanceof ExternReference) {
            if (!type.equals(ReferenceType.EXTERNREF)) {
                throw new WasmAssemblerException("Invalid type for extern reference: " + type);
            }

            ObjectType externReferenceType = ObjectType.of(ExternReference.class);

            emitter.doNew(externReferenceType);
            emitter.duplicate();
            loadConstant(emitter, type, ((ExternReference) value).getReferent());
            emitter.invoke(
                    externReferenceType,
                    "<init>",
                    new JavaType[]{ObjectType.OBJECT},
                    PrimitiveType.VOID,
                    InvokeType.SPECIAL,
                    false
            );
            return;
        }

        emitter.loadConstant(value);
    }

    @FunctionalInterface
    public interface Emitter {
        void emit() throws WasmAssemblerException;
    }
}
