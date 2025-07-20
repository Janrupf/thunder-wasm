package net.janrupf.thunderwasm.assembler.generator.defaults;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.assembler.generator.MemoryGenerator;
import net.janrupf.thunderwasm.data.Limits;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemory;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryStore;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.MemoryType;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DefaultMemoryGenerator implements MemoryGenerator {
    private static final ObjectType MEMORY_TYPE = ObjectType.of(ByteBuffer.class);
    private static final int PAGE_SIZE = 64 * 1024;

    @Override
    public void addMemory(LargeArrayIndex i, MemoryType type, ClassFileEmitter emitter) {
        emitter.field(
                generateMemoryFieldName(i),
                Visibility.PRIVATE,
                false,
                true,
                MEMORY_TYPE,
                null
        );
    }

    @Override
    public void emitMemoryConstructor(LargeArrayIndex i, MemoryType type, CodeEmitContext context) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();
        Limits limits = type.getLimits();

        frameState.pushOperand(NumberType.I32);

        emitter.loadConstant(limits.getMin() * PAGE_SIZE);
        emitter.invoke(
                MEMORY_TYPE,
                "allocateDirect",
                new JavaType[]{PrimitiveType.INT},
                MEMORY_TYPE,
                InvokeType.STATIC,
                false
        );

        frameState.popOperand(NumberType.I32);
        frameState.pushOperand(ReferenceType.OBJECT);

        frameState.pushOperand(ReferenceType.OBJECT);
        emitter.accessField(
                ObjectType.of(ByteOrder.class),
                "LITTLE_ENDIAN",
                ObjectType.of(ByteOrder.class),
                true,
                false
        );
        emitter.invoke(
                MEMORY_TYPE,
                "order",
                new JavaType[]{ObjectType.of(ByteOrder.class)},
                MEMORY_TYPE,
                InvokeType.VIRTUAL,
                false
        );
        frameState.popOperand(ReferenceType.OBJECT);

        frameState.pushOperand(ReferenceType.OBJECT);
        emitter.loadThis();
        emitter.op(Op.SWAP);
        emitter.accessField(
                context.getEmitter().getOwner(),
                generateMemoryFieldName(i),
                MEMORY_TYPE,
                false,
                true
        );

        frameState.popOperand(ReferenceType.OBJECT);
        frameState.popOperand(ReferenceType.OBJECT);
    }

    @Override
    public void emitStore(
            LargeArrayIndex i,
            MemoryType type,
            NumberType numberType,
            PlainMemory.Memarg memarg,
            PlainMemoryStore.StoreType storeType,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();
        JavaType javaType = WasmTypeConverter.toJavaType(numberType);

        String methodName;
        JavaType argumentType;
        ValueType wasmArgumentType;
        if (numberType == NumberType.I32 || numberType == NumberType.I64) {
            switch (storeType) {
                case BIT_8:
                    argumentType = PrimitiveType.BYTE;
                    wasmArgumentType = NumberType.I32;
                    methodName = "put";
                    break;

                case BIT_16:
                    argumentType = PrimitiveType.SHORT;
                    wasmArgumentType = NumberType.I32;
                    methodName = "putShort";
                    break;

                case BIT_32:
                    argumentType = PrimitiveType.INT;
                    wasmArgumentType = NumberType.I32;
                    methodName = "putInt";
                    break;

                case NATIVE:
                    argumentType = javaType;
                    wasmArgumentType = numberType;

                    if (numberType == NumberType.I32) {
                        methodName = "putInt";
                    } else { // I64
                        methodName = "putLong";
                    }

                    break;

                default:
                    throw new WasmAssemblerException("Cannot store " + numberType + " with store type " + storeType);
            }
        } else {
            if (storeType != PlainMemoryStore.StoreType.NATIVE) {
                throw new WasmAssemblerException("Cannot store " + numberType + " with store type " + storeType);
            }

            argumentType = javaType;
            wasmArgumentType = numberType;

            if (numberType == NumberType.F32) {
                methodName = "putFloat";
            } else if (numberType == NumberType.F64) {
                methodName = "putDouble";
            } else {
                throw new WasmAssemblerException("Unsupported number type for memory store " + numberType);
            }
        }

        if (!javaType.equals(argumentType)) {
            // Need to convert the value to the correct type
            if (javaType.equals(PrimitiveType.LONG)) {
                // Downcast to int, this is always required when we need to downcast to something smaller
                emitter.op(Op.L2I);
            }

            if (argumentType.equals(PrimitiveType.SHORT)) {
                emitter.op(Op.I2S);
            } else if (argumentType.equals(PrimitiveType.BYTE)) {
                emitter.op(Op.I2B);
            }
        }

        // Stow away the value, we don't need it right now
        int valueLocal = frameState.computeJavaLocalIndex(frameState.allocateLocal(wasmArgumentType));
        emitter.storeLocal(valueLocal, argumentType);
        frameState.popOperand(numberType);

        emitCalculateAccessOffset(memarg, context);

        int offsetLocal = frameState.computeJavaLocalIndex(frameState.allocateLocal(NumberType.I32));
        emitter.storeLocal(offsetLocal, PrimitiveType.INT);
        frameState.popOperand(NumberType.I32);

        frameState.pushOperand(ReferenceType.OBJECT);
        frameState.pushOperand(ReferenceType.OBJECT);
        emitter.loadThis();
        emitter.accessField(
                emitter.getOwner(),
                generateMemoryFieldName(i),
                ObjectType.of(ByteBuffer.class),
                false,
                false
        );

        frameState.popOperand(ReferenceType.OBJECT);

        emitter.loadLocal(offsetLocal, PrimitiveType.INT);
        frameState.freeLocal();
        frameState.pushOperand(NumberType.I32);

        emitter.loadLocal(valueLocal, argumentType);
        frameState.freeLocal();
        frameState.pushOperand(numberType);

        emitter.invoke(
                ObjectType.of(ByteBuffer.class),
                methodName,
                new JavaType[]{PrimitiveType.INT, argumentType},
                ObjectType.of(ByteBuffer.class),
                InvokeType.VIRTUAL,
                false
        );
        frameState.pushOperand(ReferenceType.OBJECT);
        emitter.pop(ObjectType.of(ByteBuffer.class));
        frameState.popOperand(ReferenceType.OBJECT);

        frameState.popOperand(numberType);
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(ReferenceType.OBJECT);
    }

    @Override
    public void emitLoad(
            LargeArrayIndex i,
            MemoryType type,
            NumberType numberType,
            PlainMemory.Memarg memarg,
            PlainMemoryLoad.LoadType loadType,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();
        JavaType javaType = WasmTypeConverter.toJavaType(numberType);

        String methodName;
        JavaType returnType;
        ValueType wasmReturnType;
        int bitWidth;

        if (numberType == NumberType.I32 || numberType == NumberType.I64) {
            switch (loadType) {
                case SIGNED_8:
                case UNSIGNED_8:
                    returnType = PrimitiveType.BYTE;
                    wasmReturnType = NumberType.I32;
                    methodName = "get";
                    bitWidth = 8;
                    break;

                case SIGNED_16:
                case UNSIGNED_16:
                    returnType = PrimitiveType.SHORT;
                    wasmReturnType = NumberType.I32;
                    methodName = "getShort";
                    bitWidth = 16;
                    break;

                case SIGNED_32:
                case UNSIGNED_32:
                    returnType = PrimitiveType.INT;
                    wasmReturnType = NumberType.I32;
                    methodName = "getInt";
                    bitWidth = 32;
                    break;

                case NATIVE:
                    returnType = javaType;
                    wasmReturnType = numberType;

                    if (numberType == NumberType.I32) {
                        methodName = "getInt";
                        bitWidth = 32;
                    } else { // I64
                        methodName = "getLong";
                        bitWidth = 64;
                    }

                    break;

                default:
                    throw new WasmAssemblerException("Cannot load " + numberType + " with load type " + loadType);
            }
        } else {
            if (loadType != PlainMemoryLoad.LoadType.NATIVE) {
                throw new WasmAssemblerException("Cannot load " + numberType + " with load type " + loadType);
            }

            returnType = javaType;
            wasmReturnType = numberType;

            if (numberType == NumberType.F32) {
                methodName = "getFloat";
                bitWidth = 32;
            } else if (numberType == NumberType.F64) {
                methodName = "getDouble";
                bitWidth = 64;
            } else {
                throw new WasmAssemblerException("Unsupported number type for memory load " + numberType);
            }
        }

        emitCalculateAccessOffset(memarg, context);

        frameState.pushOperand(ReferenceType.OBJECT);
        frameState.pushOperand(ReferenceType.OBJECT);
        emitter.loadThis();
        emitter.accessField(
                emitter.getOwner(),
                generateMemoryFieldName(i),
                ObjectType.of(ByteBuffer.class),
                false,
                false
        );

        frameState.popOperand(ReferenceType.OBJECT);

        frameState.popOperand(ReferenceType.OBJECT);
        frameState.popOperand(NumberType.I32);
        emitter.op(Op.SWAP); // Swap the memory with the offset
        frameState.pushOperand(NumberType.I32);
        frameState.pushOperand(ReferenceType.OBJECT);

        emitter.invoke(
                ObjectType.of(ByteBuffer.class),
                methodName,
                new JavaType[]{PrimitiveType.INT},
                returnType,
                InvokeType.VIRTUAL,
                false
        );
        frameState.popOperand(ReferenceType.OBJECT);
        frameState.pushOperand(wasmReturnType);

        if (!javaType.equals(returnType)) {
            // Need to convert the value to the correct type
            if (javaType.equals(PrimitiveType.LONG)) {
                // Upcast to long, this is always required when we need to upcast to something larger
                frameState.popOperand(wasmReturnType);
                emitter.op(Op.I2L);
                frameState.pushOperand(NumberType.I64);
            }

            if (!loadType.isSigned()) {
                // Unsigned load, we need to mask away the upper bits
                if (numberType == NumberType.I32) {
                    frameState.pushOperand(NumberType.I32);
                    emitter.loadConstant((1 << bitWidth) - 1);
                    emitter.op(Op.IAND);
                    frameState.popOperand(NumberType.I32);
                } else { // I64
                    frameState.pushOperand(NumberType.I64);
                    emitter.loadConstant((1L << bitWidth) - 1);
                    emitter.op(Op.LAND);
                    frameState.popOperand(NumberType.I64);
                }
            }
        }
    }

    protected String generateMemoryFieldName(LargeArrayIndex i) {
        if (i == null) {
            throw new IllegalArgumentException("Memory index most be specified");
        }

        return "memory_" + i;
    }

    private void emitCalculateAccessOffset(
            PlainMemory.Memarg memarg,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        if (memarg.getOffset() == 0) {
            // Nothing further to do
            return;
        }


        frameState.pushOperand(NumberType.I32);
        emitter.loadConstant(memarg.getOffset());
        emitter.op(Op.IADD);
        frameState.popOperand(NumberType.I32);
    }
}
