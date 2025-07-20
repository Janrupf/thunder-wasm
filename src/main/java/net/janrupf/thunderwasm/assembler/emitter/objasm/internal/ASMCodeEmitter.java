package net.janrupf.thunderwasm.assembler.emitter.objasm.internal;

import net.janrupf.thunderwasm.assembler.JavaFrameSnapshot;
import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.types.ArrayType;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class ASMCodeEmitter implements CodeEmitter {
    private final MethodVisitor visitor;
    private final boolean hasThisLocal;
    private final ObjectType owner;

    ASMCodeEmitter(MethodVisitor visitor, boolean hasThisLocal, ObjectType owner) {
        this.visitor = visitor;
        this.hasThisLocal = hasThisLocal;
        this.owner = owner;
    }

    @Override
    public ObjectType getOwner() {
        return owner;
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

        visitor.visitLabel(asmLabel.getInner());

        if (frame != null) {
            int localCount = frame.getLocals().size();
            int localCountWithThis = localCount + (hasThisLocal ? 1 : 0);
            int localStart = hasThisLocal ? 1 : 0;

            Object[] locals = new Object[localCountWithThis];
            if (hasThisLocal) {
                locals[0] = ASMConverter.convertType(owner).getInternalName();
            }

            for (int i = 0; i < localCount; i++) {
                locals[i + localStart] = this.javaTypeToFrameType(frame.getLocals().get(i));
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
        if (value == null) {
            visitor.visitInsn(Opcodes.ACONST_NULL);
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

                return;
            }

            if (value instanceof Byte) {
                if (this.tryPushInstr((byte) value)) {
                    return;
                }

                throw new AssertionError("Byte should be handled by tryPushInstr");
            }

            if (value instanceof Short) {
                if (this.tryPushInstr((short) value)) {
                    return;
                }

                throw new AssertionError("Short should be handled by tryPushInstr");
            }

            // Try appropriate tconst_n instructions first
            if (value instanceof Integer) {
                int iValue = (int) value;
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
                long lValue = (long) value;
                if (lValue == 0) {
                    visitor.visitInsn(Opcodes.LCONST_0);
                    return;
                } else if (lValue == 1) {
                    visitor.visitInsn(Opcodes.LCONST_1);
                    return;
                }
            } else if (value instanceof Float) {
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
                double dValue = (double) value;
                if (dValue == 0.0) {
                    visitor.visitInsn(Opcodes.DCONST_0);
                    return;
                } else if (dValue == 1.0) {
                    visitor.visitInsn(Opcodes.DCONST_1);
                    return;
                }
            }

            visitor.visitLdcInsn(value);
        } else if (value instanceof ObjectType) {
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
    public void doReturn(JavaType type) {
        if (type instanceof PrimitiveType) {
            if (type.equals(PrimitiveType.VOID)) {
                visitor.visitInsn(Opcodes.RETURN);
            } else if (type.equals(PrimitiveType.FLOAT)) {
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
    }

    @Override
    public void jump(JumpCondition condition, CodeLabel target) throws WasmAssemblerException {
        int opCode;
        switch (condition) {
            case INT_EQUAL:
                opCode = Opcodes.IF_ICMPEQ;
                break;
            case INT_NOT_EQUAL:
                opCode = Opcodes.IF_ICMPNE;
                break;
            case INT_LESS_THAN:
                opCode = Opcodes.IF_ICMPLT;
                break;
            case INT_GREATER_THAN:
                opCode = Opcodes.IF_ICMPGT;
                break;
            case INT_LESS_THAN_OR_EQUAL:
                opCode = Opcodes.IF_ICMPLE;
                break;
            case INT_GREATER_THAN_OR_EQUAL:
                opCode = Opcodes.IF_ICMPGE;
                break;
            case INT_EQUAL_ZERO:
                opCode = Opcodes.IFEQ;
                break;
            case INT_NOT_EQUAL_ZERO:
                opCode = Opcodes.IFNE;
                break;
            case INT_LESS_THAN_ZERO:
                opCode = Opcodes.IFLT;
                break;
            case INT_GREATER_THAN_ZERO:
                opCode = Opcodes.IFGT;
                break;
            case INT_LESS_THAN_OR_EQUAL_ZERO:
                opCode = Opcodes.IFLE;
                break;
            case INT_GREATER_THAN_OR_EQUAL_ZERO:
                opCode = Opcodes.IFGE;
                break;
            case REFERENCE_IS_EQUAL:
                opCode = Opcodes.IF_ACMPEQ;
                break;
            case REFERENCE_IS_NOT_EQUAL:
                opCode = Opcodes.IF_ACMPNE;
                break;
            case IS_NULL:
                opCode = Opcodes.IFNULL;
                break;
            case IS_NOT_NULL:
                opCode = Opcodes.IFNONNULL;
                break;
            case ALWAYS:
                opCode = Opcodes.GOTO;
                break;
            default:
                throw new WasmAssemblerException("Unsupported jump condition: " + condition);
        }

        visitor.visitJumpInsn(opCode, ((ASMCodeLabel) target).getInner());
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

        visitor.visitMethodInsn(
                opCode,
                asmType.getInternalName(),
                methodName,
                Type.getMethodDescriptor(asmReturnType, asmParameterTypes),
                ownerIsInterface
        );
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
    }

    @Override
    public void accessField(
            JavaType type,
            String fieldName,
            JavaType fieldType,
            boolean isStatic,
            boolean isSet
    ) {
        int opCode;
        if (isSet) {
            opCode = isStatic ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD;
        } else {
            opCode = isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD;
        }

        visitor.visitFieldInsn(
                opCode,
                ASMConverter.convertType(type).getInternalName(),
                fieldName,
                ASMConverter.convertType(fieldType).getDescriptor()
        );
    }

    public void duplicate(JavaType type) throws WasmAssemblerException {
        if (type instanceof PrimitiveType) {
            if (
                    type.equals(PrimitiveType.BYTE) ||
                            type.equals(PrimitiveType.CHAR) ||
                            type.equals(PrimitiveType.SHORT) ||
                            type.equals(PrimitiveType.INT)
            ) {
                visitor.visitInsn(Opcodes.DUP);
            } else if (type.equals(PrimitiveType.LONG)) {
                visitor.visitInsn(Opcodes.DUP2);
            } else if (type.equals(PrimitiveType.FLOAT)) {
                visitor.visitInsn(Opcodes.DUP);
            } else if (type.equals(PrimitiveType.DOUBLE)) {
                visitor.visitInsn(Opcodes.DUP2);
            } else {
                throw new WasmAssemblerException("Unsupported type: " + type);
            }
        } else {
            visitor.visitInsn(Opcodes.DUP);
        }
    }

    public void duplicate2(JavaType first, JavaType second) throws WasmAssemblerException {
        if (first instanceof PrimitiveType || second instanceof PrimitiveType) {
            if (first.getSlotCount() > 1 || second.getSlotCount() > 1) {
                throw new WasmAssemblerException("Cannot duplicate 2 values with a slot count > 1");
            }
        }

        visitor.visitInsn(Opcodes.DUP2);
    }

    public void duplicateX2(JavaType type) throws WasmAssemblerException {
        if (type instanceof PrimitiveType) {
            if (
                    type.equals(PrimitiveType.BYTE) ||
                            type.equals(PrimitiveType.CHAR) ||
                            type.equals(PrimitiveType.SHORT) ||
                            type.equals(PrimitiveType.INT)
            ) {
                visitor.visitInsn(Opcodes.DUP_X2);
            } else if (type.equals(PrimitiveType.LONG)) {
                visitor.visitInsn(Opcodes.DUP2_X2);
            } else if (type.equals(PrimitiveType.FLOAT)) {
                visitor.visitInsn(Opcodes.DUP_X2);
            } else if (type.equals(PrimitiveType.DOUBLE)) {
                visitor.visitInsn(Opcodes.DUP2_X2);
            } else {
                throw new WasmAssemblerException("Unsupported type: " + type);
            }
        } else {
            visitor.visitInsn(Opcodes.DUP_X2);
        }
    }

    public void pop(JavaType type) throws WasmAssemblerException {
        if (type instanceof PrimitiveType) {
            if (
                    type.equals(PrimitiveType.BYTE) ||
                            type.equals(PrimitiveType.CHAR) ||
                            type.equals(PrimitiveType.SHORT) ||
                            type.equals(PrimitiveType.INT)
            ) {
                visitor.visitInsn(Opcodes.POP);
            } else if (type.equals(PrimitiveType.LONG)) {
                visitor.visitInsn(Opcodes.POP2);
            } else if (type.equals(PrimitiveType.FLOAT)) {
                visitor.visitInsn(Opcodes.POP);
            } else if (type.equals(PrimitiveType.DOUBLE)) {
                visitor.visitInsn(Opcodes.POP2);
            } else {
                throw new WasmAssemblerException("Unsupported type: " + type);
            }
        } else {
            visitor.visitInsn(Opcodes.POP);
        }
    }

    @Override
    public void storeArrayElement(ArrayType arrayType) {
        JavaType elementType = arrayType.getElementType();

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
    }

    @Override
    public void loadArrayElement(ArrayType arrayType) {
        JavaType elementType = arrayType.getElementType();

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
    }

    @Override
    public void op(Op op) throws WasmAssemblerException {
        int opCode;

        switch (op) {
            case IADD:
                opCode = Opcodes.IADD;
                break;

            case ISUB:
                opCode = Opcodes.ISUB;
                break;

            case IMUL:
                opCode = Opcodes.IMUL;
                break;

            case IDIV:
                opCode = Opcodes.IDIV;
                break;

            case IREM:
                opCode = Opcodes.IREM;
                break;

            case IXOR:
                opCode = Opcodes.IXOR;
                break;

            case ISHL:
                opCode = Opcodes.ISHL;
                break;

            case ISHR:
                opCode = Opcodes.ISHR;
                break;

            case IUSHR:
                opCode = Opcodes.IUSHR;
                break;

            case IAND:
                opCode = Opcodes.IAND;
                break;

            case IOR:
                opCode = Opcodes.IOR;
                break;

            case SWAP:
                opCode = Opcodes.SWAP;
                break;

            case LADD:
                opCode = Opcodes.LADD;
                break;

            case LSUB:
                opCode = Opcodes.LSUB;
                break;

            case LMUL:
                opCode = Opcodes.LMUL;
                break;

            case LDIV:
                opCode = Opcodes.LDIV;
                break;

            case LREM:
                opCode = Opcodes.LREM;
                break;

            case LAND:
                opCode = Opcodes.LAND;
                break;

            case LOR:
                opCode = Opcodes.LOR;
                break;

            case LXOR:
                opCode = Opcodes.LXOR;
                break;

            case LSHL:
                opCode = Opcodes.LSHL;
                break;

            case LSHR:
                opCode = Opcodes.LSHR;
                break;

            case LUSHR:
                opCode = Opcodes.LUSHR;
                break;

            case LCMP:
                opCode = Opcodes.LCMP;
                break;

            case FNEG:
                opCode = Opcodes.FNEG;
                break;

            case FADD:
                opCode = Opcodes.FADD;
                break;

            case FSUB:
                opCode = Opcodes.FSUB;
                break;

            case FMUL:
                opCode = Opcodes.FMUL;
                break;

            case FDIV:
                opCode = Opcodes.FDIV;
                break;

            case FCMPG:
                opCode = Opcodes.FCMPG;
                break;

            case FCMPL:
                opCode = Opcodes.FCMPL;
                break;

            case DNEG:
                opCode = Opcodes.DNEG;
                break;

            case DADD:
                opCode = Opcodes.DADD;
                break;

            case DSUB:
                opCode = Opcodes.DSUB;
                break;

            case DMUL:
                opCode = Opcodes.DMUL;
                break;

            case DDIV:
                opCode = Opcodes.DDIV;
                break;

            case DCMPG:
                opCode = Opcodes.DCMPG;
                break;

            case DCMPL:
                opCode = Opcodes.DCMPL;
                break;

            case I2L:
                opCode = Opcodes.I2L;
                break;

            case I2F:
                opCode = Opcodes.I2F;
                break;

            case I2D:
                opCode = Opcodes.I2D;
                break;

            case I2B:
                opCode = Opcodes.I2B;
                break;

            case I2S:
                opCode = Opcodes.I2S;
                break;

            case L2I:
                opCode = Opcodes.L2I;
                break;

            case L2F:
                opCode = Opcodes.L2F;
                break;

            case L2D:
                opCode = Opcodes.L2D;
                break;

            case F2D:
                opCode = Opcodes.F2D;
                break;

            case D2F:
                opCode = Opcodes.D2F;
                break;

            case F2I:
                opCode = Opcodes.F2I;
                break;

            case F2L:
                opCode = Opcodes.F2L;
                break;

            case D2I:
                opCode = Opcodes.D2I;
                break;

            case D2L:
                opCode = Opcodes.D2L;
                break;

            default:
                throw new WasmAssemblerException("Unsupported operation: " + op);
        }

        visitor.visitInsn(opCode);
    }

    @Override
    public void checkCast(ObjectType type) {
        visitor.visitTypeInsn(Opcodes.CHECKCAST, ASMConverter.convertType(type).getInternalName());
    }

    @Override
    public void loadThis() throws WasmAssemblerException {
        if (!hasThisLocal) {
            throw new WasmAssemblerException("No this local available");
        }

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
    }

    @Override
    public void loadLocal(int index, JavaType type) throws WasmAssemblerException {
        if (hasThisLocal) {
            index++;
        }

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

        visitor.visitVarInsn(opCode, index);
    }

    public void storeLocal(int index, JavaType type) throws WasmAssemblerException {
        if (hasThisLocal) {
            index++;
        }

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

        visitor.visitVarInsn(opCode, index);
    }

    @Override
    public void finish(int maxOperands, int maxLocals) {
        if (hasThisLocal) {
            maxLocals++;
        }

        visitor.visitMaxs(maxOperands, maxLocals);
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
}
