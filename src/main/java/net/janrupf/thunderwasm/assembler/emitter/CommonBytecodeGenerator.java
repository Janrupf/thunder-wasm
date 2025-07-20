package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.data.Limits;
import net.janrupf.thunderwasm.runtime.ElementReference;
import net.janrupf.thunderwasm.runtime.ExternReference;
import net.janrupf.thunderwasm.runtime.FunctionReference;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;
import net.janrupf.thunderwasm.types.VecType;

import java.util.function.BiConsumer;

public class CommonBytecodeGenerator {
    private CommonBytecodeGenerator() {
    }

    /**
     * Convert the top i32 from unsigned to signed, swapping them in the process.
     *
     * @param frameState the current frame state
     * @param emitter    the code emitter
     * @throws WasmAssemblerException if the conversion is invalid
     */
    public static void swapConvertUnsignedI32(
            WasmFrameState frameState,
            CodeEmitter emitter
    ) throws WasmAssemblerException {
        // Notify the state that we will push and pop an additional i32
        frameState.pushOperand(NumberType.I32);
        frameState.popOperand(NumberType.I32);

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
     * @param frameState the current frame state
     * @param emitter    the code emitter
     * @param type       the type of the values to convert
     * @throws WasmAssemblerException if the conversion is invalid
     */
    public static void convertTop2Unsigned(
            WasmFrameState frameState,
            CodeEmitter emitter,
            NumberType type
    ) throws WasmAssemblerException {
        Op addOp;

        if (type.equals(NumberType.I64)) {
            addOp = Op.LADD;
        } else {
            throw new WasmAssemblerException("Unsupported type for unsigned conversion: " + type);
        }

        int localIndex = frameState.computeJavaLocalIndex(frameState.allocateLocal(type));

        // Convert the value on top
        frameState.pushOperand(type);
        emitter.loadConstant(type.getMinValue());
        emitter.op(addOp);

        // Store the converted value in a local
        emitter.storeLocal(localIndex, WasmTypeConverter.toJavaType(type));

        // Now convert the value which was below
        emitter.loadConstant(type.getMinValue());
        emitter.op(addOp);

        // Load back the local
        emitter.loadLocal(localIndex, WasmTypeConverter.toJavaType(type));

        frameState.popOperand(type);

        frameState.freeLocal();
    }

    /**
     * Helper function to generate a conditional jump that pushes 0 or 1 on the stack.
     *
     * @param frameState the current frame state
     * @param emitter    the code emitter
     * @param condition  the condition to evaluate
     * @throws WasmAssemblerException if the condition is invalid
     */
    public static void evalConditionZeroOrOne(
            WasmFrameState frameState,
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
        emitter.resolveLabel(successLabel, frameState.computeSnapshot());
        emitter.loadConstant(1);

        // Finished
        frameState.pushOperand(NumberType.I32);
        emitter.resolveLabel(finishedLabel, frameState.computeSnapshot());
    }

    /**
     * Helper function to evaluate a comparison result and push 0 or 1 on the stack
     * depending on whether the result matches the target result.
     *
     * @param frameState   the current frame state
     * @param emitter      the code emitter
     * @param targetResult the target result to compare against
     * @throws WasmAssemblerException if the comparison cannot be generated
     */
    public static void evalCompResultZeroOrOne(
            WasmFrameState frameState,
            CodeEmitter emitter,
            ComparisonResult targetResult
    ) throws WasmAssemblerException {
        switch (targetResult) {
            case LESS_THAN:
                frameState.pushOperand(NumberType.I32);

                // Shift amount to the right by 31 bits, this only leaves -1 to be 1
                emitter.loadConstant(31);
                emitter.op(Op.IUSHR);

                frameState.popOperand(NumberType.I32);
                break;
            case GREATER_THAN:
                frameState.pushOperand(NumberType.I32);

                // Add 1 to get 1 or 2
                emitter.loadConstant(1);
                emitter.op(Op.IADD);

                // Shift the value to the right by 1 bit
                emitter.loadConstant(1);
                emitter.op(Op.IUSHR);

                frameState.popOperand(NumberType.I32);
                break;
            case EQUAL:
                frameState.pushOperand(NumberType.I32);

                // Xor the value with -1 to invert all bits
                emitter.loadConstant(-1);
                emitter.op(Op.IXOR);

                // And with 1 to get 1 if the value was 0
                emitter.loadConstant(1);
                emitter.op(Op.IAND);

                frameState.popOperand(NumberType.I32);
                break;
            case LESS_THAN_OR_EQUAL:
                frameState.pushOperand(NumberType.I32);

                // Subtract 1 from the value
                emitter.loadConstant(1);
                emitter.op(Op.ISUB);

                // Shift amount to the right by 31 bits, this only leaves -1 or -2 to be 1
                emitter.loadConstant(31);
                emitter.op(Op.IUSHR);

                frameState.popOperand(NumberType.I32);
                break;
            case GREATER_THAN_OR_EQUAL:
                frameState.pushOperand(NumberType.I32);

                // Add 2 to get 2 or 3
                emitter.loadConstant(2);
                emitter.op(Op.IADD);

                // Shift the value to the right by 1 bit
                emitter.loadConstant(1);
                emitter.op(Op.IUSHR);

                frameState.popOperand(NumberType.I32);
                break;
            case NOT_EQUAL:
                frameState.pushOperand(NumberType.I32);

                // And with 1 to get 1 if the value was -1 or 1
                emitter.loadConstant(1);
                emitter.op(Op.IAND);

                frameState.popOperand(NumberType.I32);
                break;
        }
    }

    /**
     * Evaluate if any of 2 floats on top of the stack is NaN.
     *
     * @param frameState the current frame state
     * @param emitter    the code emitter
     * @param target     the target result to generate
     * @throws WasmAssemblerException if the evaluation cannot be generated
     */
    public static void eval2FloatsNaN(
            WasmFrameState frameState,
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

        frameState.pushOperand(NumberType.F32);
        frameState.pushOperand(NumberType.F32);
        frameState.pushOperand(NumberType.F32);

        // Get copies of both floats to check for NaN
        emitter.duplicate2(PrimitiveType.FLOAT, PrimitiveType.FLOAT);

        // Check the first float for NaN by duplicating it and comparing it to itself
        emitter.duplicate(PrimitiveType.FLOAT);
        emitter.op(compareOp);

        // Get the second float on top and compare it to itself
        emitter.op(Op.SWAP);
        emitter.duplicate(PrimitiveType.FLOAT);
        emitter.op(compareOp);

        // Combine both results to check if either of the floats is NaN
        emitter.op(Op.IOR);

        // Invert -1 to 0 and 0 to -1
        if (target == NaNTargetResult.ZERO) {
            emitter.loadConstant(-1);
            emitter.op(Op.IXOR);
        }

        frameState.popOperand(NumberType.F32);
        frameState.popOperand(NumberType.F32);
        frameState.popOperand(NumberType.F32);

        frameState.pushOperand(NumberType.I32);
    }

    /**
     * Generate a float comparison that always returns 0 if NaN is detected.
     *
     * @param frameState   the current frame state
     * @param emitter      the code emitter
     * @param targetResult the target result to generate
     * @throws WasmAssemblerException if the evaluation cannot be generated
     */
    public static void evalFloatCompNaNZero(
            WasmFrameState frameState,
            CodeEmitter emitter,
            ComparisonResult targetResult
    ) throws WasmAssemblerException {
        // Check for NaN, pushing 0 if NaN is detected, -1 otherwise
        CommonBytecodeGenerator.eval2FloatsNaN(frameState, emitter, NaNTargetResult.ZERO);

        // Move the NaN check result 2 values down and then discard the top copy
        emitter.duplicateX2(PrimitiveType.INT);
        emitter.pop(PrimitiveType.INT);
        frameState.popOperand(NumberType.I32);

        frameState.popOperand(NumberType.F32);
        frameState.popOperand(NumberType.F32);
        emitter.op(Op.FCMPG);
        frameState.pushOperand(NumberType.I32);

        CommonBytecodeGenerator.evalCompResultZeroOrOne(frameState, emitter, targetResult);

        // AND the two results
        emitter.op(Op.IAND);
    }

    /**
     * Evaluate if any of 2 doubles on top of the stack is NaN.
     *
     * @param frameState the current frame state
     * @param emitter    the code emitter
     * @param target     the target result to generate
     * @throws WasmAssemblerException if the evaluation cannot be generated
     */
    public static void eval2DoublesNaN(
            WasmFrameState frameState,
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

        frameState.popOperand(NumberType.F64);
        frameState.popOperand(NumberType.F64);
        frameState.pushOperand(NumberType.I32);
        frameState.pushOperand(NumberType.F64);
        frameState.pushOperand(NumberType.F64);

        int firstLocal = frameState.computeJavaLocalIndex(frameState.allocateLocal(NumberType.F64));
        int secondLocal = frameState.computeJavaLocalIndex(frameState.allocateLocal(NumberType.F64));
        int nanStateLocal = frameState.computeJavaLocalIndex(frameState.allocateLocal(NumberType.I32));

        // Store the original doubles into locals
        emitter.storeLocal(secondLocal, WasmTypeConverter.toJavaType(NumberType.F64));
        emitter.storeLocal(firstLocal, WasmTypeConverter.toJavaType(NumberType.F64));

        // Load the second double and check it for NaN
        emitter.loadLocal(secondLocal, WasmTypeConverter.toJavaType(NumberType.F64));
        emitter.duplicate(PrimitiveType.DOUBLE);

        // Check for NaN and store the result
        emitter.op(compareOp);
        emitter.storeLocal(nanStateLocal, WasmTypeConverter.toJavaType(NumberType.I32));

        // Load the first double and check it for NaN
        emitter.loadLocal(firstLocal, WasmTypeConverter.toJavaType(NumberType.F64));
        emitter.duplicate(PrimitiveType.DOUBLE);

        // Check for NaN
        emitter.op(compareOp);

        // Load the previous result
        emitter.loadLocal(nanStateLocal, WasmTypeConverter.toJavaType(NumberType.I32));

        // Combine both results to check if either of the doubles is NaN
        emitter.op(Op.IOR);

        // Invert -1 to 0 and 0 to -1
        if (target == NaNTargetResult.ZERO) {
            emitter.loadConstant(-1);
            emitter.op(Op.IXOR);
        }

        // Get back the doubles
        emitter.loadLocal(firstLocal, WasmTypeConverter.toJavaType(NumberType.F64));
        emitter.loadLocal(secondLocal, WasmTypeConverter.toJavaType(NumberType.F64));

        // Free the locals
        frameState.freeLocal();
        frameState.freeLocal();
        frameState.freeLocal();
    }

    /**
     * Generate a double comparison that always returns 0 if NaN is detected.
     *
     * @param frameState   the current frame state
     * @param emitter      the code emitter
     * @param targetResult the target result to generate
     * @throws WasmAssemblerException if the evaluation cannot be generated
     */
    public static void evalDoubleCompNaNZero(
            WasmFrameState frameState,
            CodeEmitter emitter,
            ComparisonResult targetResult
    ) throws WasmAssemblerException {
        // Check for NaN, pushing 0 if NaN is detected, -1 otherwise
        CommonBytecodeGenerator.eval2DoublesNaN(frameState, emitter, NaNTargetResult.ZERO);

        frameState.popOperand(NumberType.F64);
        frameState.popOperand(NumberType.F64);
        emitter.op(Op.DCMPG);
        frameState.pushOperand(NumberType.I32);

        CommonBytecodeGenerator.evalCompResultZeroOrOne(frameState, emitter, targetResult);

        // AND the two results
        emitter.op(Op.IAND);
        frameState.popOperand(NumberType.I32);
    }

    /**
     * Swap the top two values on the stack.
     *
     * @param frameState the current frame state
     * @param emitter    the code emitter
     * @throws WasmAssemblerException if the swap cannot be generated
     */
    public static void swap(
            WasmFrameState frameState,
            CodeEmitter emitter
    ) throws WasmAssemblerException {
        ValueType top = frameState.popAnyOperand();
        ValueType below = frameState.popAnyOperand();

        JavaType topType = WasmTypeConverter.toJavaType(top);
        JavaType belowType = WasmTypeConverter.toJavaType(below);

        if (topType.getSlotCount() < 2 && belowType.getSlotCount() < 2) {
            // Simple swap
            emitter.op(Op.SWAP);
        } else {
            // Swap using locals
            int topLocal = frameState.computeJavaLocalIndex(frameState.allocateLocal(top));
            int belowLocal = frameState.computeJavaLocalIndex(frameState.allocateLocal(below));

            emitter.storeLocal(topLocal, topType);
            emitter.storeLocal(belowLocal, belowType);

            emitter.loadLocal(topLocal, topType);
            emitter.loadLocal(belowLocal, belowType);

            frameState.freeLocal();
            frameState.freeLocal();
        }

        frameState.pushOperand(top);
        frameState.pushOperand(below);
    }

    /**
     * Load the value of "this" and move it below n values.
     *
     * @param frameState      the current frame state
     * @param emitter         the code emitter
     * @param valuesAboveThis the number of values above "this"
     * @throws WasmAssemblerException if the load cannot be generated
     */
    public static void loadThisBelow(
            WasmFrameState frameState,
            CodeEmitter emitter,
            int valuesAboveThis
    ) throws WasmAssemblerException {
        loadBelow(frameState, emitter, valuesAboveThis, ReferenceType.OBJECT, emitter::loadThis);
    }

    /**
     * Load a value and move it below n values.
     *
     * @param frameState      the current frame state
     * @param emitter         the code emitter
     * @param valuesAbove     the number of values above the value
     * @param type            the type of the value
     * @param emitterFunction the function to invoke to generate the emitting instructions,
     *                        the emitter should not manipulate the frame state
     * @throws WasmAssemblerException if the load cannot be generated
     */
    public static void loadBelow(
            WasmFrameState frameState,
            CodeEmitter emitter,
            int valuesAbove,
            ValueType type,
            Emitter emitterFunction
    ) throws WasmAssemblerException {
        if (valuesAbove == 0) {
            emitterFunction.emit();
            frameState.pushOperand(type);
            return;
        }

        // Remove all values above
        ValueType[] toPop = new ValueType[valuesAbove];
        for (int i = 0; i < valuesAbove; i++) {
            toPop[i] = frameState.popAnyOperand();
        }

        JavaType firstToPop = WasmTypeConverter.toJavaType(toPop[0]);
        JavaType toPush = WasmTypeConverter.toJavaType(type);
        if (toPop.length == 1 && firstToPop.getSlotCount() < 2 && toPush.getSlotCount() < 2) {
            // Simple swap
            frameState.pushOperand(type);
            frameState.pushOperand(toPop[0]);
            emitterFunction.emit();
            emitter.op(Op.SWAP);
        } else {
            // Transfer the values to locals
            int[] locals = new int[toPop.length];
            for (int i = 0; i < toPop.length; i++) {
                locals[i] = frameState.computeJavaLocalIndex(frameState.allocateLocal(toPop[i]));
                emitter.storeLocal(locals[i], WasmTypeConverter.toJavaType(toPop[i]));
            }

            // Load value and push it
            frameState.pushOperand(type);
            emitterFunction.emit();

            // Load the values back
            for (int i = toPop.length - 1; i >= 0; i--) {
                emitter.loadLocal(locals[i], WasmTypeConverter.toJavaType(toPop[i]));
                frameState.freeLocal();
                frameState.pushOperand(toPop[i]);
            }
        }
    }

    /**
     * Load the value type reference.
     *
     * @param frameState the current frame state
     * @param emitter    the code emitter
     * @param type       the value type
     * @throws WasmAssemblerException if the load cannot be generated
     */
    public static void loadTypeReference(
            WasmFrameState frameState,
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

        frameState.pushOperand(ReferenceType.OBJECT);
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
     * @param frameState the current frame state
     * @param emitter    the code emitter
     * @param limits     the limits to load
     * @throws WasmAssemblerException if the load cannot be generated
     */
    public static void loadLimits(WasmFrameState frameState, CodeEmitter emitter, Limits limits)
            throws WasmAssemblerException {
        frameState.pushOperand(ReferenceType.OBJECT);
        frameState.pushOperand(ReferenceType.OBJECT);

        // Construct the limits instance
        emitter.doNew(ObjectType.of(Limits.class));
        emitter.duplicate(ObjectType.of(Limits.class));

        // Push min (int) and max (Integer)
        frameState.pushOperand(NumberType.I32);
        emitter.loadConstant(limits.getMin());

        if (limits.getMax() == null) {
            frameState.pushOperand(ReferenceType.OBJECT);
            emitter.loadConstant(null);
        } else {
            // Need to box!
            frameState.pushOperand(NumberType.I32);
            emitter.loadConstant(limits.getMax());
            emitter.invoke(
                    ObjectType.of(Integer.class),
                    "valueOf",
                    new JavaType[]{PrimitiveType.INT},
                    ObjectType.of(Integer.class),
                    InvokeType.STATIC,
                    false
            );

            frameState.popOperand(NumberType.I32);
            frameState.pushOperand(ReferenceType.OBJECT);
        }

        emitter.invoke(
                ObjectType.of(Limits.class),
                "<init>",
                new JavaType[]{PrimitiveType.INT, ObjectType.of(Integer.class)},
                PrimitiveType.VOID,
                InvokeType.SPECIAL,
                false
        );

        frameState.popOperand(ReferenceType.OBJECT);
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(ReferenceType.OBJECT);
    }

    /**
     * Load a constant value.
     * <p>
     * This is mostly equivalent to {@link CodeEmitter#loadConstant(Object)} but also supports
     * {@link net.janrupf.thunderwasm.runtime.FunctionReference} and
     * {@link net.janrupf.thunderwasm.runtime.ExternReference} values.
     *
     * @param emitter    the code emitter
     * @param frameState the current frame state
     * @param type       the type of the value
     * @param value      the value to load
     * @throws WasmAssemblerException if the constant type is not supported
     */
    public static void loadConstant(
            CodeEmitter emitter,
            WasmFrameState frameState,
            ValueType type,
            Object value
    ) throws WasmAssemblerException {
        if (value instanceof FunctionReference) {
            if (!type.equals(ReferenceType.FUNCREF)) {
                throw new WasmAssemblerException("Invalid type for function reference: " + type);
            }

            ObjectType functionReferenceType = ObjectType.of(FunctionReference.class);

            frameState.pushOperand(NumberType.I32);
            emitter.loadConstant(((FunctionReference) value).getFunctionIndex());

            emitter.invoke(
                    functionReferenceType,
                    "of",
                    new JavaType[]{PrimitiveType.INT},
                    functionReferenceType,
                    InvokeType.STATIC,
                    false
            );

            frameState.popOperand(NumberType.I32);
            frameState.pushOperand(ReferenceType.FUNCREF);
            return;
        } else if (value instanceof ExternReference) {
            if (!type.equals(ReferenceType.EXTERNREF)) {
                throw new WasmAssemblerException("Invalid type for extern reference: " + type);
            }

            ObjectType externReferenceType = ObjectType.of(ExternReference.class);

            frameState.pushOperand(ReferenceType.OBJECT);
            frameState.pushOperand(ReferenceType.OBJECT);
            emitter.doNew(externReferenceType);
            emitter.duplicate(externReferenceType);
            loadConstant(emitter, frameState, type, ((ExternReference) value).getReferent());
            emitter.invoke(
                    externReferenceType,
                    "<init>",
                    new JavaType[]{ObjectType.OBJECT},
                    PrimitiveType.VOID,
                    InvokeType.SPECIAL,
                    false
            );
            frameState.popOperand(ReferenceType.OBJECT);
            frameState.popOperand(ReferenceType.OBJECT);
            frameState.popOperand(ReferenceType.OBJECT);
            frameState.pushOperand(ReferenceType.EXTERNREF);
            return;
        }

        frameState.pushOperand(type);
        emitter.loadConstant(value);
    }

    @FunctionalInterface
    public interface Emitter {
        void emit() throws WasmAssemblerException;
    }
}
