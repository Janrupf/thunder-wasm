package net.janrupf.thunderwasm.assembler.emitter.objasm.internal;

import net.janrupf.thunderwasm.assembler.JavaFrameSnapshot;
import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaStackFrameState;
import net.janrupf.thunderwasm.assembler.emitter.types.ArrayType;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class ASMCodeEmitter implements CodeEmitter {
    private final MethodVisitor visitor;

    // Java always has the this local at index 0, however, for WASM generated code this is somewhat
    // impractical because WASM has the argument order reversed. In order to accommodate for that,
    // "this" may sometimes be passed in another position.
    private final ObjectType owner;
    private final JavaType returnType;
    private final JavaLocal thisLocal;
    private final JavaLocal[] staticLocals;
    private JavaStackFrameState stackFrameState;
    private boolean noNewInstructionsSinceLastVisitFrame;

    private int maxLocalsSize;
    private int maxStackSize;

    ASMCodeEmitter(
            MethodVisitor visitor,
            ObjectType owner,
            JavaType returnType,
            JavaLocal thisLocal,
            JavaLocal[] staticLocals,
            JavaStackFrameState stackFrameState
    ) {
        this.visitor = visitor;
        this.owner = owner;
        this.returnType = returnType;
        this.thisLocal = thisLocal;
        this.staticLocals = staticLocals;
        this.stackFrameState = stackFrameState;
        this.noNewInstructionsSinceLastVisitFrame = false;

        this.maxLocalsSize = 0;
        this.maxStackSize = 0;
    }

    @Override
    public ObjectType getOwner() {
        return owner;
    }

    @Override
    public JavaStackFrameState getStackFrameState() {
        return stackFrameState;
    }

    @Override
    public JavaFrameSnapshot fixupInferredFrame(JavaFrameSnapshot snapshot) throws WasmAssemblerException {
        // We need to re-order the static locals and `this` - the first N locals of the snapshot always need to
        // be the static locals and this. Additional locals may have been allocated later and are not re-ordered
        // by this emitter.
        List<JavaType> newLocals = new ArrayList<>(snapshot.getLocals());

        List<JavaLocal> specialLocals = new ArrayList<>();
        if (thisLocal != null) {
            specialLocals.add(thisLocal);
        }
        specialLocals.addAll(Arrays.asList(staticLocals));

        if (newLocals.size() < staticLocals.length) {
            // This shouldn't happen unless someone freed the argument locals
            throw new WasmAssemblerException("Argument locals have disappeared from frame state");
        }

        // Sort by slot index and overwrite the re-ordered locals
        specialLocals.sort(Comparator.comparingInt(JavaLocal::getSlot));

        // Make room for the additional this local
        if (thisLocal != null) {
            newLocals.add(0, ObjectType.OBJECT);
        }
        for (int i = 0; i < specialLocals.size(); i++) {
            newLocals.set(i, specialLocals.get(i).getType());
        }

        // The emitter doesn't do anything special with the stack, just use it as is
        return new JavaFrameSnapshot(snapshot.getStack(), newLocals);
    }

    @Override
    public CodeLabel newLabel() {
        return new ASMCodeLabel();
    }

    @Override
    public void resolveLabel(CodeLabel label, JavaFrameSnapshot frame) throws WasmAssemblerException {
        ASMCodeLabel asmLabel = (ASMCodeLabel) label;
        asmLabel.checkNotResolved();
        asmLabel.markResolved();

        if (frame != null) {
            asmLabel.attachFrameState(frame);
        } else {
            frame = asmLabel.getKnownFrameSnapshot();
        }

        if (stackFrameState == null) {
            if (frame == null) {
                throw new WasmAssemblerException("Current frame state is not known");
            }

            stackFrameState = new JavaStackFrameState();
            stackFrameState.restoreFromSnapshot(asmLabel.getKnownFrameSnapshot());
        } else if (frame != null) {
            stackFrameState.computeSnapshot().checkCompatible(frame);
        } else {
            frame = stackFrameState.computeSnapshot();
            asmLabel.attachFrameState(frame);
        }

        requireValidFrameSnapshot();

        visitor.visitLabel(asmLabel.getInner());

        if (!noNewInstructionsSinceLastVisitFrame) {
            int localCount = frame.getLocals().size();

            Object[] locals = new Object[localCount];

            for (int i = 0; i < localCount; i++) {
                locals[i] = this.javaTypeToFrameType(frame.getLocals().get(i));
            }

            Object[] stack = new Object[frame.getStack().size()];
            for (int i = 0; i < stack.length; i++) {
                stack[i] = this.javaTypeToFrameType(frame.getStack().get(i));
            }

            visitor.visitFrame(
                    Opcodes.F_FULL,
                    locals.length,
                    locals,
                    stack.length,
                    stack
            );

            noNewInstructionsSinceLastVisitFrame = true;
        }
    }

    private Object javaTypeToFrameType(JavaType type) throws WasmAssemblerException {
        if (type instanceof PrimitiveType) {
            if (type.equals(PrimitiveType.VOID)) {
                throw new WasmAssemblerException("Cannot convert void type to frame type");
            } else if (type.equals(PrimitiveType.FLOAT)) {
                return Opcodes.FLOAT;
            } else if (type.equals(PrimitiveType.DOUBLE)) {
                return Opcodes.DOUBLE;
            } else if (type.equals(PrimitiveType.LONG)) {
                return Opcodes.LONG;
            } else {
                return Opcodes.INTEGER;
            }
        } else {
            return ASMConverter.convertType(type).getInternalName();
        }
    }

    @Override
    public void loadConstant(Object value) throws WasmAssemblerException {
        requireValidFrameSnapshot();
        markNewInstruction();

        if (value == null) {
            visitor.visitInsn(Opcodes.ACONST_NULL);

            // TODO: This could cause issues in stack frames,
            //       we should probably reject null here and provide
            //       a specialized "loadNull()" method
            stackFrameState.pushOperand(ObjectType.OBJECT);
        } else if (value instanceof Boolean ||
                value instanceof Byte ||
                value instanceof Short ||
                value instanceof Integer ||
                value instanceof Long ||
                value instanceof Float ||
                value instanceof Double ||
                value instanceof String
        ) {
            if (value instanceof Boolean) {
                if ((boolean) value) {
                    visitor.visitInsn(Opcodes.ICONST_1);
                } else {
                    visitor.visitInsn(Opcodes.ICONST_0);
                }

                stackFrameState.pushOperand(PrimitiveType.INT);
                return;
            }

            if (value instanceof Byte) {
                if (this.tryPushInstr((byte) value)) {
                    stackFrameState.pushOperand(PrimitiveType.INT);
                    return;
                }

                throw new AssertionError("Byte should be handled by tryPushInstr");
            }

            if (value instanceof Short) {
                if (this.tryPushInstr((short) value)) {
                    stackFrameState.pushOperand(PrimitiveType.INT);
                    return;
                }

                throw new AssertionError("Short should be handled by tryPushInstr");
            }

            // Try appropriate tconst_n instructions first
            if (value instanceof Integer) {
                int iValue = (int) value;
                stackFrameState.pushOperand(PrimitiveType.INT);

                switch (iValue) {
                    case -1: {
                        visitor.visitInsn(Opcodes.ICONST_M1);
                        return;
                    }
                    case 0: {
                        visitor.visitInsn(Opcodes.ICONST_0);
                        return;
                    }
                    case 1: {
                        visitor.visitInsn(Opcodes.ICONST_1);
                        return;
                    }
                    case 2: {
                        visitor.visitInsn(Opcodes.ICONST_2);
                        return;
                    }
                    case 3: {
                        visitor.visitInsn(Opcodes.ICONST_3);
                        return;
                    }
                    case 4: {
                        visitor.visitInsn(Opcodes.ICONST_4);
                        return;
                    }
                    case 5: {
                        visitor.visitInsn(Opcodes.ICONST_5);
                        return;
                    }
                    default: {
                        if (this.tryPushInstr(iValue)) {
                            return;
                        }
                    }
                }
            } else if (value instanceof Long) {
                stackFrameState.pushOperand(PrimitiveType.LONG);

                long lValue = (long) value;
                if (lValue == 0) {
                    visitor.visitInsn(Opcodes.LCONST_0);
                    return;
                } else if (lValue == 1) {
                    visitor.visitInsn(Opcodes.LCONST_1);
                    return;
                }
            } else if (value instanceof Float) {
                stackFrameState.pushOperand(PrimitiveType.FLOAT);

                float fValue = (float) value;
                if (fValue == 0.0f) {
                    visitor.visitInsn(Opcodes.FCONST_0);
                    return;
                } else if (fValue == 1.0f) {
                    visitor.visitInsn(Opcodes.FCONST_1);
                    return;
                } else if (fValue == 2.0f) {
                    visitor.visitInsn(Opcodes.FCONST_2);
                    return;
                }
            } else if (value instanceof Double) {
                stackFrameState.pushOperand(PrimitiveType.DOUBLE);

                double dValue = (double) value;
                if (dValue == 0.0) {
                    visitor.visitInsn(Opcodes.DCONST_0);
                    return;
                } else if (dValue == 1.0) {
                    visitor.visitInsn(Opcodes.DCONST_1);
                    return;
                }
            }

            if (value instanceof String) {
                stackFrameState.pushOperand(ObjectType.of(String.class));
            }
            visitor.visitLdcInsn(value);
        } else if (value instanceof ObjectType) {
            stackFrameState.pushOperand(ObjectType.of(Class.class));
            visitor.visitLdcInsn(ASMConverter.convertType((ObjectType) value));
        } else {
            throw new WasmAssemblerException("Unsupported constant type: " + value.getClass().getName());
        }
    }

    private boolean tryPushInstr(int value) {
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            visitor.visitIntInsn(Opcodes.BIPUSH, value);
            return true;
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            visitor.visitIntInsn(Opcodes.SIPUSH, value);
            return true;
        }
        return false;
    }

    @Override
    public void doReturn() throws WasmAssemblerException {
        requireValidFrameSnapshot();
        markNewInstruction();

        if (returnType.equals(PrimitiveType.VOID)) {
            visitor.visitInsn(Opcodes.RETURN);
            invalidateCurrentFrameState();
            return;
        }

        JavaType type = stackFrameState.popOperand(returnType);

        if (type instanceof PrimitiveType) {
            if (type.equals(PrimitiveType.FLOAT)) {
                visitor.visitInsn(Opcodes.FRETURN);
            } else if (type.equals(PrimitiveType.DOUBLE)) {
                visitor.visitInsn(Opcodes.DRETURN);
            } else if (type.equals(PrimitiveType.LONG)) {
                visitor.visitInsn(Opcodes.LRETURN);
            } else {
                visitor.visitInsn(Opcodes.IRETURN);
            }
        } else {
            visitor.visitInsn(Opcodes.ARETURN);
        }

        invalidateCurrentFrameState();
    }

    @Override
    public void jump(JumpCondition condition, CodeLabel target) throws WasmAssemblerException {
        requireValidFrameSnapshot();

        int opCode;
        switch (condition) {
            case INT_EQUAL:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.INT);
                opCode = Opcodes.IF_ICMPEQ;
                break;
            case INT_NOT_EQUAL:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.INT);
                opCode = Opcodes.IF_ICMPNE;
                break;
            case INT_LESS_THAN:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.INT);
                opCode = Opcodes.IF_ICMPLT;
                break;
            case INT_GREATER_THAN:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.INT);
                opCode = Opcodes.IF_ICMPGT;
                break;
            case INT_LESS_THAN_OR_EQUAL:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.INT);
                opCode = Opcodes.IF_ICMPLE;
                break;
            case INT_GREATER_THAN_OR_EQUAL:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.INT);
                opCode = Opcodes.IF_ICMPGE;
                break;
            case INT_EQUAL_ZERO:
                stackFrameState.popOperand(PrimitiveType.INT);
                opCode = Opcodes.IFEQ;
                break;
            case INT_NOT_EQUAL_ZERO:
                stackFrameState.popOperand(PrimitiveType.INT);
                opCode = Opcodes.IFNE;
                break;
            case INT_LESS_THAN_ZERO:
                stackFrameState.popOperand(PrimitiveType.INT);
                opCode = Opcodes.IFLT;
                break;
            case INT_GREATER_THAN_ZERO:
                stackFrameState.popOperand(PrimitiveType.INT);
                opCode = Opcodes.IFGT;
                break;
            case INT_LESS_THAN_OR_EQUAL_ZERO:
                stackFrameState.popOperand(PrimitiveType.INT);
                opCode = Opcodes.IFLE;
                break;
            case INT_GREATER_THAN_OR_EQUAL_ZERO:
                stackFrameState.popOperand(PrimitiveType.INT);
                opCode = Opcodes.IFGE;
                break;
            case REFERENCE_IS_EQUAL:
                stackFrameState.popOperands(ObjectType.OBJECT, ObjectType.OBJECT);
                opCode = Opcodes.IF_ACMPEQ;
                break;
            case REFERENCE_IS_NOT_EQUAL:
                stackFrameState.popOperands(ObjectType.OBJECT, ObjectType.OBJECT);
                opCode = Opcodes.IF_ACMPNE;
                break;
            case IS_NULL:
                stackFrameState.popOperand(ObjectType.OBJECT);
                opCode = Opcodes.IFNULL;
                break;
            case IS_NOT_NULL:
                stackFrameState.popOperand(ObjectType.OBJECT);
                opCode = Opcodes.IFNONNULL;
                break;
            case ALWAYS:
                opCode = Opcodes.GOTO;
                break;
            default:
                throw new WasmAssemblerException("Unsupported jump condition: " + condition);
        }

        ASMCodeLabel label = (ASMCodeLabel) target;
        label.attachFrameState(stackFrameState.computeSnapshot());

        visitor.visitJumpInsn(opCode, ((ASMCodeLabel) target).getInner());
        markNewInstruction();

        if (condition == JumpCondition.ALWAYS) {
            invalidateCurrentFrameState();
        }
    }

    @Override
    public void invoke(
            JavaType type,
            String methodName,
            JavaType[] parameterTypes,
            JavaType returnType,
            InvokeType invokeType,
            boolean ownerIsInterface
    ) throws WasmAssemblerException {
        requireValidFrameSnapshot();

        int opCode;
        switch (invokeType) {
            case INTERFACE:
                opCode = Opcodes.INVOKEINTERFACE;
                break;
            case SPECIAL:
                opCode = Opcodes.INVOKESPECIAL;
                break;
            case STATIC:
                opCode = Opcodes.INVOKESTATIC;
                break;
            case VIRTUAL:
                opCode = Opcodes.INVOKEVIRTUAL;
                break;
            default:
                throw new WasmAssemblerException("Unsupported invoke type: " + invokeType);
        }

        Type asmType = ASMConverter.convertType(type);
        Type asmReturnType = ASMConverter.convertType(returnType);
        Type[] asmParameterTypes = ASMConverter.convertTypes(parameterTypes);

        for (int i = parameterTypes.length - 1; i >= 0; i--) {
            stackFrameState.popOperand(parameterTypes[i]);
        }

        if (!invokeType.equals(InvokeType.STATIC)) {
            stackFrameState.popOperand(type);
        }

        visitor.visitMethodInsn(
                opCode,
                asmType.getInternalName(),
                methodName,
                Type.getMethodDescriptor(asmReturnType, asmParameterTypes),
                ownerIsInterface
        );
        markNewInstruction();

        if (!returnType.equals(PrimitiveType.VOID)) {
            stackFrameState.pushOperand(returnType);
        }
    }

    @Override
    public void doNew(ObjectType type) {
        if (type instanceof ArrayType) {
            JavaType elementType = ((ArrayType) type).getElementType();

            if (elementType instanceof PrimitiveType) {
                PrimitiveType primitiveType = (PrimitiveType) elementType;
                visitor.visitIntInsn(Opcodes.NEWARRAY, primitiveToJvmT(primitiveType));
            } else {
                visitor.visitTypeInsn(Opcodes.ANEWARRAY, ASMConverter.convertType(elementType).getInternalName());
            }
        } else {
            visitor.visitTypeInsn(Opcodes.NEW, ASMConverter.convertType(type).getInternalName());
        }

        markNewInstruction();
        stackFrameState.pushOperand(type);
    }

    @Override
    public void accessField(
            JavaType type,
            String fieldName,
            JavaType fieldType,
            boolean isStatic,
            boolean isSet
    ) throws WasmAssemblerException {
        requireValidFrameSnapshot();

        int opCode;
        if (isSet) {
            opCode = isStatic ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD;
        } else {
            opCode = isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD;
        }

        if (isSet) {
            stackFrameState.popOperand(fieldType);
        }

        if (!isStatic) {
            stackFrameState.popOperand(type);
        }

        visitor.visitFieldInsn(
                opCode,
                ASMConverter.convertType(type).getInternalName(),
                fieldName,
                ASMConverter.convertType(fieldType).getDescriptor()
        );
        markNewInstruction();

        if (!isSet) {
            stackFrameState.pushOperand(fieldType);
        }
    }

    @Override
    public void duplicate(int count, int depth) throws WasmAssemblerException {
        requireValidFrameSnapshot();

        if (count > 2 || count < 1) {
            throw new WasmAssemblerException("Invalid number of duplicants " + count);
        }

        if (depth < 0 || depth > 2) {
            throw new WasmAssemblerException("Invalid duplication depth " + depth);
        }

        // I'm not sure if this is better than a chain of else-if-else-if in
        // terms of readability, but it's not way worse either.
        //
        // To be read cas: case (count << 3) | depth
        switch ((count << 3) | depth) {
            case (1 << 3) | 0: {
                JavaType type = stackFrameState.requireOperand();

                if (type.getSlotCount() > 1) {
                    visitor.visitInsn(Opcodes.DUP2);
                } else {
                    visitor.visitInsn(Opcodes.DUP);
                }

                stackFrameState.pushOperand(type);
                break;
            }

            case (1 << 3) | 1: {
                JavaType topType = stackFrameState.popAnyOperand();
                JavaType belowType = stackFrameState.popAnyOperand();

                if (topType.getSlotCount() == 1 && belowType.getSlotCount() == 1) {
                    visitor.visitInsn(Opcodes.DUP_X1);
                } else if (topType.getSlotCount() > 1 && belowType.getSlotCount() == 1) {
                    visitor.visitInsn(Opcodes.DUP2_X1);
                } else if (topType.getSlotCount() == 1 && belowType.getSlotCount() > 1) {
                    visitor.visitInsn(Opcodes.DUP_X2);
                } else {
                    visitor.visitInsn(Opcodes.DUP2_X2);
                }

                // ..., below, top → ..., top, below, top
                stackFrameState.pushOperands(topType, belowType, topType);
                break;
            }

            case (1 << 3) | 2: {
                JavaType topType = stackFrameState.popAnyOperand();
                JavaType depth1Type = stackFrameState.popAnyOperand();
                JavaType depth2Type = stackFrameState.popAnyOperand();

                if (depth1Type.getSlotCount() > 1 || depth2Type.getSlotCount() > 1) {
                    throw new WasmAssemblerException("Cannot duplicate 1 element 2 down over category 2 elements");
                }

                if (topType.getSlotCount() == 1) {
                    visitor.visitInsn(Opcodes.DUP_X2);
                } else {
                    visitor.visitInsn(Opcodes.DUP2_X2);
                }

                stackFrameState.pushOperands(topType, depth2Type, depth1Type, topType);

                break;
            }

            case (2 << 3) | 0: {
                JavaType topType = stackFrameState.requireOperand(0);
                JavaType secondType = stackFrameState.requireOperand(1);

                if (topType.getSlotCount() == 1 && secondType.getSlotCount() == 1) {
                    // dup2: ..., second, top → ..., second, top, second, top
                    visitor.visitInsn(Opcodes.DUP2);
                    stackFrameState.pushOperands(secondType, topType);
                } else {
                    throw new WasmAssemblerException("Cannot duplicate 2 elements when not both category 1");
                }
                break;
            }

            case (2 << 3) | 1: {
                JavaType topType = stackFrameState.requireOperand(0);
                JavaType secondType = stackFrameState.requireOperand(1);
                JavaType belowType = stackFrameState.requireOperand(2);

                if (topType.getSlotCount() != 1 || secondType.getSlotCount() != 1) {
                    throw new WasmAssemblerException("Cannot duplicate 2 elements when not both category 1");
                }

                if (belowType.getSlotCount() == 1) {
                    // dup2_x1: ..., below, second, top → ..., second, top, below, second, top
                    visitor.visitInsn(Opcodes.DUP2_X1);
                } else {
                    // dup2_x2: ..., below, second, top → ..., second, top, below, second, top
                    visitor.visitInsn(Opcodes.DUP2_X2);
                }

                stackFrameState.popOperands(topType, secondType, belowType);
                stackFrameState.pushOperands(secondType, topType, belowType, secondType, topType);
                break;
            }

            case (2 << 3) | 2: {
                JavaType topType = stackFrameState.popAnyOperand();
                JavaType depth1Type = stackFrameState.popAnyOperand();
                JavaType depth2Type = stackFrameState.popAnyOperand();
                JavaType depth3Type = stackFrameState.popAnyOperand();

                if (topType.getSlotCount() == 1 && depth1Type.getSlotCount() == 1) {
                    if (depth2Type.getSlotCount() > 1 || depth3Type.getSlotCount() > 1) {
                        throw new WasmAssemblerException("Cannot 2 elements duplicate 2 down over a category 2 element");
                    }

                    visitor.visitInsn(Opcodes.DUP2_X2);
                    stackFrameState.pushOperands(depth1Type, topType, depth3Type, depth2Type, depth1Type, topType);
                } else {
                    throw new WasmAssemblerException("Cannot duplicate 2 elements when not both category 1");
                }
                break;
            }

            default:
                throw new WasmAssemblerException("Unsupported duplication count " + count + " with depth " + depth);
        }

        markNewInstruction();
    }


    @Override
    public void pop() throws WasmAssemblerException {
        requireValidFrameSnapshot();

        JavaType type = stackFrameState.popAnyOperand();

        if (type.getSlotCount() > 1) {
            visitor.visitInsn(Opcodes.POP2);
        } else {
            visitor.visitInsn(Opcodes.POP);
        }

        markNewInstruction();
    }

    @Override
    public void storeArrayElement() throws WasmAssemblerException {
        requireValidFrameSnapshot();

        JavaType elementTypeOnStack = stackFrameState.popAnyOperand();
        stackFrameState.popOperand(PrimitiveType.INT);
        JavaType arrayType = stackFrameState.popAnyOperand();

        if (!(arrayType instanceof ArrayType)) {
            throw new WasmAssemblerException("Expected array type at stack depth 1");
        }

        JavaType elementType = ((ArrayType) arrayType).getElementType();

        if (!stackFrameState.remapForStackFrame(elementType).equals(elementTypeOnStack)) {
            throw new WasmAssemblerException("Expected operand of type " + elementType + " on the stack but found " + elementTypeOnStack);
        }

        if (elementType.equals(PrimitiveType.BOOLEAN)) {
            visitor.visitInsn(Opcodes.BASTORE);
        } else if (elementType.equals(PrimitiveType.CHAR)) {
            visitor.visitInsn(Opcodes.CASTORE);
        } else if (elementType.equals(PrimitiveType.FLOAT)) {
            visitor.visitInsn(Opcodes.FASTORE);
        } else if (elementType.equals(PrimitiveType.DOUBLE)) {
            visitor.visitInsn(Opcodes.DASTORE);
        } else if (elementType.equals(PrimitiveType.BYTE)) {
            visitor.visitInsn(Opcodes.BASTORE);
        } else if (elementType.equals(PrimitiveType.SHORT)) {
            visitor.visitInsn(Opcodes.SASTORE);
        } else if (elementType.equals(PrimitiveType.INT)) {
            visitor.visitInsn(Opcodes.IASTORE);
        } else if (elementType.equals(PrimitiveType.LONG)) {
            visitor.visitInsn(Opcodes.LASTORE);
        } else {
            visitor.visitInsn(Opcodes.AASTORE);
        }

        markNewInstruction();
    }

    @Override
    public void loadArrayElement() throws WasmAssemblerException {
        requireValidFrameSnapshot();

        stackFrameState.popOperand(PrimitiveType.INT);
        JavaType arrayType = stackFrameState.popAnyOperand();
        if (!(arrayType instanceof ArrayType)) {
            throw new WasmAssemblerException("Expected array type at stack depth 1");
        }

        JavaType elementType = ((ArrayType) arrayType).getElementType();

        if (elementType.equals(PrimitiveType.BOOLEAN)) {
            visitor.visitInsn(Opcodes.BALOAD);
        } else if (elementType.equals(PrimitiveType.CHAR)) {
            visitor.visitInsn(Opcodes.CALOAD);
        } else if (elementType.equals(PrimitiveType.FLOAT)) {
            visitor.visitInsn(Opcodes.FALOAD);
        } else if (elementType.equals(PrimitiveType.DOUBLE)) {
            visitor.visitInsn(Opcodes.DALOAD);
        } else if (elementType.equals(PrimitiveType.BYTE)) {
            visitor.visitInsn(Opcodes.BALOAD);
        } else if (elementType.equals(PrimitiveType.SHORT)) {
            visitor.visitInsn(Opcodes.SALOAD);
        } else if (elementType.equals(PrimitiveType.INT)) {
            visitor.visitInsn(Opcodes.IALOAD);
        } else if (elementType.equals(PrimitiveType.LONG)) {
            visitor.visitInsn(Opcodes.LALOAD);
        } else {
            visitor.visitInsn(Opcodes.AALOAD);
        }

        markNewInstruction();
    }

    @Override
    public void op(Op op) throws WasmAssemblerException {
        requireValidFrameSnapshot();

        int opCode;

        switch (op) {
            case IADD:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.INT);
                opCode = Opcodes.IADD;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case ISUB:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.INT);
                opCode = Opcodes.ISUB;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case IMUL:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.INT);
                opCode = Opcodes.IMUL;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case IDIV:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.INT);
                opCode = Opcodes.IDIV;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case IREM:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.INT);
                opCode = Opcodes.IREM;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case IXOR:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.INT);
                opCode = Opcodes.IXOR;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case ISHL:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.INT);
                opCode = Opcodes.ISHL;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case ISHR:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.INT);
                opCode = Opcodes.ISHR;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case IUSHR:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.INT);
                opCode = Opcodes.IUSHR;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case IAND:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.INT);
                opCode = Opcodes.IAND;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case IOR:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.INT);
                opCode = Opcodes.IOR;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case SWAP: {
                JavaType first = stackFrameState.popAnyOperand();
                if (first.getSlotCount() > 1) {
                    throw new WasmAssemblerException("Unexpected computational type 2 value at depth 0");
                }

                JavaType second = stackFrameState.popAnyOperand();
                if (second.getSlotCount() > 1) {
                    throw new WasmAssemblerException("Unexpected computational type 2 value at depth 1");
                }

                opCode = Opcodes.SWAP;

                stackFrameState.pushOperand(first);
                stackFrameState.pushOperand(second);
                break;
            }

            case LADD:
                stackFrameState.popOperands(PrimitiveType.LONG, PrimitiveType.LONG);
                opCode = Opcodes.LADD;
                stackFrameState.pushOperand(PrimitiveType.LONG);
                break;

            case LSUB:
                stackFrameState.popOperands(PrimitiveType.LONG, PrimitiveType.LONG);
                opCode = Opcodes.LSUB;
                stackFrameState.pushOperand(PrimitiveType.LONG);
                break;

            case LMUL:
                stackFrameState.popOperands(PrimitiveType.LONG, PrimitiveType.LONG);
                opCode = Opcodes.LMUL;
                stackFrameState.pushOperand(PrimitiveType.LONG);
                break;

            case LDIV:
                stackFrameState.popOperands(PrimitiveType.LONG, PrimitiveType.LONG);
                opCode = Opcodes.LDIV;
                stackFrameState.pushOperand(PrimitiveType.LONG);
                break;

            case LREM:
                stackFrameState.popOperands(PrimitiveType.LONG, PrimitiveType.LONG);
                opCode = Opcodes.LREM;
                stackFrameState.pushOperand(PrimitiveType.LONG);
                break;

            case LAND:
                stackFrameState.popOperands(PrimitiveType.LONG, PrimitiveType.LONG);
                opCode = Opcodes.LAND;
                stackFrameState.pushOperand(PrimitiveType.LONG);
                break;

            case LOR:
                stackFrameState.popOperands(PrimitiveType.LONG, PrimitiveType.LONG);
                opCode = Opcodes.LOR;
                stackFrameState.pushOperand(PrimitiveType.LONG);
                break;

            case LXOR:
                stackFrameState.popOperands(PrimitiveType.LONG, PrimitiveType.LONG);
                opCode = Opcodes.LXOR;
                stackFrameState.pushOperand(PrimitiveType.LONG);
                break;

            case LSHL:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.LONG);
                opCode = Opcodes.LSHL;
                stackFrameState.pushOperand(PrimitiveType.LONG);
                break;

            case LSHR:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.LONG);
                opCode = Opcodes.LSHR;
                stackFrameState.pushOperand(PrimitiveType.LONG);
                break;

            case LUSHR:
                stackFrameState.popOperands(PrimitiveType.INT, PrimitiveType.LONG);
                opCode = Opcodes.LUSHR;
                stackFrameState.pushOperand(PrimitiveType.LONG);
                break;

            case LCMP:
                stackFrameState.popOperands(PrimitiveType.LONG, PrimitiveType.LONG);
                opCode = Opcodes.LCMP;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case FNEG:
                stackFrameState.popOperand(PrimitiveType.FLOAT);
                opCode = Opcodes.FNEG;
                stackFrameState.pushOperand(PrimitiveType.FLOAT);
                break;

            case FADD:
                stackFrameState.popOperands(PrimitiveType.FLOAT, PrimitiveType.FLOAT);
                opCode = Opcodes.FADD;
                stackFrameState.pushOperand(PrimitiveType.FLOAT);
                break;

            case FSUB:
                stackFrameState.popOperands(PrimitiveType.FLOAT, PrimitiveType.FLOAT);
                opCode = Opcodes.FSUB;
                stackFrameState.pushOperand(PrimitiveType.FLOAT);
                break;

            case FMUL:
                stackFrameState.popOperands(PrimitiveType.FLOAT, PrimitiveType.FLOAT);
                opCode = Opcodes.FMUL;
                stackFrameState.pushOperand(PrimitiveType.FLOAT);
                break;

            case FDIV:
                stackFrameState.popOperands(PrimitiveType.FLOAT, PrimitiveType.FLOAT);
                opCode = Opcodes.FDIV;
                stackFrameState.pushOperand(PrimitiveType.FLOAT);
                break;

            case FCMPG:
                stackFrameState.popOperands(PrimitiveType.FLOAT, PrimitiveType.FLOAT);
                opCode = Opcodes.FCMPG;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case FCMPL:
                stackFrameState.popOperands(PrimitiveType.FLOAT, PrimitiveType.FLOAT);
                opCode = Opcodes.FCMPL;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case DNEG:
                stackFrameState.popOperand(PrimitiveType.DOUBLE);
                opCode = Opcodes.DNEG;
                stackFrameState.pushOperand(PrimitiveType.DOUBLE);
                break;

            case DADD:
                stackFrameState.popOperands(PrimitiveType.DOUBLE, PrimitiveType.DOUBLE);
                opCode = Opcodes.DADD;
                stackFrameState.pushOperand(PrimitiveType.DOUBLE);
                break;

            case DSUB:
                stackFrameState.popOperands(PrimitiveType.DOUBLE, PrimitiveType.DOUBLE);
                opCode = Opcodes.DSUB;
                stackFrameState.pushOperand(PrimitiveType.DOUBLE);
                break;

            case DMUL:
                stackFrameState.popOperands(PrimitiveType.DOUBLE, PrimitiveType.DOUBLE);
                opCode = Opcodes.DMUL;
                stackFrameState.pushOperand(PrimitiveType.DOUBLE);
                break;

            case DDIV:
                stackFrameState.popOperands(PrimitiveType.DOUBLE, PrimitiveType.DOUBLE);
                opCode = Opcodes.DDIV;
                stackFrameState.pushOperand(PrimitiveType.DOUBLE);
                break;

            case DCMPG:
                stackFrameState.popOperands(PrimitiveType.DOUBLE, PrimitiveType.DOUBLE);
                opCode = Opcodes.DCMPG;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case DCMPL:
                stackFrameState.popOperands(PrimitiveType.DOUBLE, PrimitiveType.DOUBLE);
                opCode = Opcodes.DCMPL;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case I2L:
                stackFrameState.popOperand(PrimitiveType.INT);
                opCode = Opcodes.I2L;
                stackFrameState.pushOperand(PrimitiveType.LONG);
                break;

            case I2F:
                stackFrameState.popOperand(PrimitiveType.INT);
                opCode = Opcodes.I2F;
                stackFrameState.pushOperand(PrimitiveType.FLOAT);
                break;

            case I2D:
                stackFrameState.popOperand(PrimitiveType.INT);
                opCode = Opcodes.I2D;
                stackFrameState.pushOperand(PrimitiveType.DOUBLE);
                break;

            case I2B:
                stackFrameState.popOperand(PrimitiveType.INT);
                opCode = Opcodes.I2B;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case I2S:
                stackFrameState.popOperand(PrimitiveType.INT);
                opCode = Opcodes.I2S;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case L2I:
                stackFrameState.popOperand(PrimitiveType.LONG);
                opCode = Opcodes.L2I;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case L2F:
                stackFrameState.popOperand(PrimitiveType.LONG);
                opCode = Opcodes.L2F;
                stackFrameState.pushOperand(PrimitiveType.FLOAT);
                break;

            case L2D:
                stackFrameState.popOperand(PrimitiveType.LONG);
                opCode = Opcodes.L2D;
                stackFrameState.pushOperand(PrimitiveType.DOUBLE);
                break;

            case F2D:
                stackFrameState.popOperand(PrimitiveType.FLOAT);
                opCode = Opcodes.F2D;
                stackFrameState.pushOperand(PrimitiveType.DOUBLE);
                break;

            case D2F:
                stackFrameState.popOperand(PrimitiveType.DOUBLE);
                opCode = Opcodes.D2F;
                stackFrameState.pushOperand(PrimitiveType.FLOAT);
                break;

            case F2I:
                stackFrameState.popOperand(PrimitiveType.FLOAT);
                opCode = Opcodes.F2I;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case F2L:
                stackFrameState.popOperand(PrimitiveType.FLOAT);
                opCode = Opcodes.F2L;
                stackFrameState.pushOperand(PrimitiveType.LONG);
                break;

            case D2I:
                stackFrameState.popOperand(PrimitiveType.DOUBLE);
                opCode = Opcodes.D2I;
                stackFrameState.pushOperand(PrimitiveType.INT);
                break;

            case D2L:
                stackFrameState.popOperand(PrimitiveType.DOUBLE);
                opCode = Opcodes.D2L;
                stackFrameState.pushOperand(PrimitiveType.LONG);
                break;

            case THROW:
                stackFrameState.popOperand(ObjectType.OBJECT);
                opCode = Opcodes.ATHROW;
                break;

            default:
                throw new WasmAssemblerException("Unsupported operation: " + op);
        }

        visitor.visitInsn(opCode);
        markNewInstruction();

        if (op == Op.THROW) {
            invalidateCurrentFrameState();
        }
    }

    @Override
    public void checkCast(ObjectType type) throws WasmAssemblerException {
        requireValidFrameSnapshot();

        stackFrameState.popOperand(ObjectType.OBJECT);
        visitor.visitTypeInsn(Opcodes.CHECKCAST, ASMConverter.convertType(type).getInternalName());
        markNewInstruction();
        stackFrameState.pushOperand(type);
    }

    @Override
    public void tableSwitch(int base, CodeLabel defaultLabel, CodeLabel... targets) throws WasmAssemblerException {
        requireValidFrameSnapshot();

        stackFrameState.popOperand(PrimitiveType.INT);
        JavaFrameSnapshot snapshot = stackFrameState.computeSnapshot();

        ((ASMCodeLabel) defaultLabel).attachFrameState(snapshot);

        Label asmDefaultLabel = ((ASMCodeLabel) defaultLabel).getInner();
        Label[] targetLabels = new Label[targets.length];

        for (int i = 0; i < targetLabels.length; i++) {
            targetLabels[i] = ((ASMCodeLabel) targets[i]).getInner();
            ((ASMCodeLabel) targets[i]).attachFrameState(snapshot);
        }

        visitor.visitTableSwitchInsn(base, base + targetLabels.length - 1, asmDefaultLabel, targetLabels);
        markNewInstruction();
        invalidateCurrentFrameState();
    }

    @Override
    public void loadThis() throws WasmAssemblerException {
        requireValidFrameSnapshot();

        if (thisLocal == null) {
            throw new WasmAssemblerException("Can not load this inside a static function");
        }

        visitor.visitVarInsn(Opcodes.ALOAD, thisLocal.getSlot());
        markNewInstruction();
        stackFrameState.pushOperand(owner);
    }

    @Override
    public JavaLocal getStaticLocal(int index) throws WasmAssemblerException {
        requireValidFrameSnapshot();

        if (index >= this.staticLocals.length) {
            throw new WasmAssemblerException("Argument index " + index + " out of bounds");
        }

        return this.staticLocals[index];
    }

    @Override
    public JavaLocal allocateLocal(JavaType type) throws WasmAssemblerException {
        requireValidFrameSnapshot();

        return stackFrameState.allocateLocal(type);
    }

    @Override
    public void loadLocal(JavaLocal local) throws WasmAssemblerException {
        requireValidFrameSnapshot();

        if (!local.isValid()) {
            throw new WasmAssemblerException("Local has been invalidated");
        }

        JavaType type = local.getType();

        int opCode;

        if (type instanceof PrimitiveType) {
            if (
                    type.equals(PrimitiveType.BYTE) ||
                            type.equals(PrimitiveType.CHAR) ||
                            type.equals(PrimitiveType.SHORT) ||
                            type.equals(PrimitiveType.INT)
            ) {
                opCode = Opcodes.ILOAD;
            } else if (type.equals(PrimitiveType.LONG)) {
                opCode = Opcodes.LLOAD;
            } else if (type.equals(PrimitiveType.FLOAT)) {
                opCode = Opcodes.FLOAD;
            } else if (type.equals(PrimitiveType.DOUBLE)) {
                opCode = Opcodes.DLOAD;
            } else {
                throw new WasmAssemblerException("Unsupported local type: " + type);
            }
        } else if (type.equals(PrimitiveType.VOID)) {
            throw new WasmAssemblerException("Cannot load void type");
        } else {
            opCode = Opcodes.ALOAD;
        }

        visitor.visitVarInsn(opCode, local.getSlot());
        markNewInstruction();
        stackFrameState.pushOperand(type);
    }

    @Override
    public void storeLocal(JavaLocal local) throws WasmAssemblerException {
        requireValidFrameSnapshot();

        if (!local.isValid()) {
            throw new WasmAssemblerException("Local has been invalidated");
        }

        JavaType type = local.getType();

        int opCode;

        if (type instanceof PrimitiveType) {
            if (
                    type.equals(PrimitiveType.BYTE) ||
                            type.equals(PrimitiveType.CHAR) ||
                            type.equals(PrimitiveType.SHORT) ||
                            type.equals(PrimitiveType.INT)
            ) {
                opCode = Opcodes.ISTORE;
            } else if (type.equals(PrimitiveType.LONG)) {
                opCode = Opcodes.LSTORE;
            } else if (type.equals(PrimitiveType.FLOAT)) {
                opCode = Opcodes.FSTORE;
            } else if (type.equals(PrimitiveType.DOUBLE)) {
                opCode = Opcodes.DSTORE;
            } else {
                throw new WasmAssemblerException("Unsupported local type: " + type);
            }
        } else if (type.equals(PrimitiveType.VOID)) {
            throw new WasmAssemblerException("Cannot store void type");
        } else {
            opCode = Opcodes.ASTORE;
        }

        stackFrameState.popOperand(type);
        visitor.visitVarInsn(opCode, local.getSlot());
        markNewInstruction();
    }

    @Override
    public void finish() throws WasmAssemblerException {
        if (stackFrameState != null) {
            markNewInstruction();
            invalidateCurrentFrameState();
        }

        visitor.visitMaxs(maxStackSize, maxLocalsSize);
        visitor.visitEnd();
    }

    private int primitiveToJvmT(PrimitiveType type) {
        if (type == PrimitiveType.BOOLEAN) {
            return Opcodes.T_BOOLEAN;
        }
        if (type == PrimitiveType.BYTE) {
            return Opcodes.T_BYTE;
        }
        if (type == PrimitiveType.CHAR) {
            return Opcodes.T_CHAR;
        }
        if (type == PrimitiveType.SHORT) {
            return Opcodes.T_SHORT;
        }
        if (type == PrimitiveType.INT) {
            return Opcodes.T_INT;
        }
        if (type == PrimitiveType.LONG) {
            return Opcodes.T_LONG;
        }
        if (type == PrimitiveType.FLOAT) {
            return Opcodes.T_FLOAT;
        }
        if (type == PrimitiveType.DOUBLE) {
            return Opcodes.T_DOUBLE;
        }

        throw new IllegalArgumentException("The JVM doesn't have an int representation of the primitive type " + type);
    }

    private void requireValidFrameSnapshot() throws WasmAssemblerException {
        if (stackFrameState == null) {
            throw new WasmAssemblerException("Stack frame state is currently unknown");
        }
    }

    private void invalidateCurrentFrameState() throws WasmAssemblerException {
        requireValidFrameSnapshot();

        if (stackFrameState.maxStackSize() > this.maxStackSize) {
            this.maxStackSize = stackFrameState.maxStackSize();
        }

        if (stackFrameState.maxLocalsSize() > this.maxLocalsSize) {
            this.maxLocalsSize = stackFrameState.maxLocalsSize();
        }

        this.stackFrameState = null;
    }

    private void markNewInstruction() {
        noNewInstructionsSinceLastVisitFrame = false;
    }
}
