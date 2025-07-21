package net.janrupf.thunderwasm.assembler.generator.defaults;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.ArrayType;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.assembler.generator.MemoryGenerator;
import net.janrupf.thunderwasm.data.Limits;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemory;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryStore;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.encoding.LargeByteArray;
import net.janrupf.thunderwasm.module.section.segment.DataSegment;
import net.janrupf.thunderwasm.types.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DefaultMemoryGenerator implements MemoryGenerator {
    private static final ArrayType BYTE_ARRAY_TYPE = new ArrayType(PrimitiveType.BYTE);
    private static final ArrayType DATA_SEGMENT_TYPE = BYTE_ARRAY_TYPE;
    private static final ObjectType MEMORY_TYPE = ObjectType.of(ByteBuffer.class);
    private static final int PAGE_SIZE = 64 * 1024;

    private final String fieldName;

    public DefaultMemoryGenerator() {
        this.fieldName = null;
    }

    public DefaultMemoryGenerator(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public void addMemory(LargeArrayIndex i, MemoryType type, ClassFileEmitter emitter) {
        emitter.field(
                generateMemoryFieldName(i),
                Visibility.PRIVATE,
                false,
                false,
                MEMORY_TYPE,
                null
        );
    }

    @Override
    public void addDataSegment(LargeArrayIndex i, DataSegment segment, ClassFileEmitter emitter) {
        emitter.field(
                generateDataSegmentFieldName(i),
                Visibility.PRIVATE,
                false,
                true,
                DATA_SEGMENT_TYPE,
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

        emitEnforceByteOrder(context);
        emitAccessMemoryField(i, true, context);
    }

    @Override
    public void emitDataSegmentConstructor(LargeArrayIndex i, DataSegment segment, CodeEmitContext context) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();
        LargeByteArray data = segment.getInit();

        if (data.length() > Integer.MAX_VALUE) {
            throw new WasmAssemblerException("Data segment is too large: " + data.length());
        }

        // Create a new byte array for the data segment
        frameState.pushOperand(ReferenceType.OBJECT);
        frameState.pushOperand(ReferenceType.OBJECT);

        frameState.pushOperand(NumberType.I32);
        emitter.loadConstant((int) data.length());
        frameState.popOperand(NumberType.I32);

        emitter.doNew(DATA_SEGMENT_TYPE);
        frameState.popOperand(ReferenceType.OBJECT);

        frameState.pushOperand(ReferenceType.OBJECT);
        frameState.pushOperand(NumberType.I32);
        frameState.pushOperand(NumberType.I32);

        for (LargeArrayIndex j = LargeArrayIndex.ZERO; j.compareTo(data.largeLength()) < 0; j = j.add(1)) {
            emitter.duplicate();

            // Load the byte value from the data segment
            emitter.loadConstant(j.getElementIndex());
            emitter.loadConstant(data.get(j));
            emitter.storeArrayElement();
        }

        frameState.popOperand(NumberType.I32);
        frameState.popOperand(NumberType.I32);

        frameState.pushOperand(ReferenceType.OBJECT);
        emitter.loadThis();
        emitter.op(Op.SWAP);
        emitter.accessField(
                context.getEmitter().getOwner(),
                generateDataSegmentFieldName(i),
                DATA_SEGMENT_TYPE,
                false,
                true
        );

        frameState.popOperand(ReferenceType.OBJECT);
        frameState.popOperand(ReferenceType.OBJECT);
    }

    @Override
    public void emitMemoryInit(
            LargeArrayIndex memoryIndex,
            MemoryType type,
            LargeArrayIndex dataIndex,
            DataSegment segment,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        /* This method expects the following stack top:
         * - count
         * - source start index
         * - destination start index
         */

        JavaLocal countLocal = emitter.allocateLocal(PrimitiveType.INT);
        emitter.storeLocal(countLocal);
        frameState.popOperand(NumberType.I32);

        JavaLocal sourceStartLocal = emitter.allocateLocal(PrimitiveType.INT);
        emitter.storeLocal(sourceStartLocal);
        frameState.popOperand(NumberType.I32);

        JavaLocal destinationStartLocal = emitter.allocateLocal(PrimitiveType.INT);
        emitter.storeLocal(destinationStartLocal);
        frameState.popOperand(NumberType.I32);

        frameState.pushOperand(ReferenceType.OBJECT);
        emitter.loadThis();

        frameState.pushOperand(ReferenceType.OBJECT);
        emitter.duplicate();

        emitter.accessField(
                emitter.getOwner(),
                generateMemoryFieldName(memoryIndex),
                MEMORY_TYPE,
                false,
                false
        );

        emitter.op(Op.SWAP);

        emitter.accessField(
                emitter.getOwner(),
                generateDataSegmentFieldName(dataIndex),
                DATA_SEGMENT_TYPE,
                false,
                false
        );

        frameState.pushOperand(NumberType.I32);
        emitter.loadLocal(destinationStartLocal);

        frameState.popOperand(NumberType.I32);
        frameState.popOperand(ReferenceType.OBJECT);
        emitter.op(Op.SWAP);
        frameState.pushOperand(NumberType.I32);
        frameState.pushOperand(ReferenceType.OBJECT);

        frameState.pushOperand(NumberType.I32);
        emitter.loadLocal(sourceStartLocal);

        frameState.pushOperand(NumberType.I32);
        emitter.loadLocal(countLocal);

        emitter.invoke(
                MEMORY_TYPE,
                "put",
                new JavaType[]{PrimitiveType.INT, DATA_SEGMENT_TYPE, PrimitiveType.INT, PrimitiveType.INT},
                MEMORY_TYPE,
                InvokeType.VIRTUAL,
                false
        );

        frameState.popOperand(NumberType.I32);
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(ReferenceType.OBJECT);
        frameState.popOperand(NumberType.I32);

        emitter.pop();
        frameState.popOperand(ReferenceType.OBJECT);

        // Free the locals
        countLocal.free();
        sourceStartLocal.free();
        destinationStartLocal.free();
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
        JavaLocal valueLocal = emitter.allocateLocal(WasmTypeConverter.toJavaType(wasmArgumentType));
        emitter.storeLocal(valueLocal);
        frameState.popOperand(numberType);

        emitCalculateAccessOffset(memarg, context);

        JavaLocal offsetLocal = emitter.allocateLocal(PrimitiveType.INT);
        emitter.storeLocal(offsetLocal);
        frameState.popOperand(NumberType.I32);

        frameState.pushOperand(ReferenceType.OBJECT);
        emitAccessMemoryField(i, false, context);

        emitter.loadLocal(offsetLocal);
        offsetLocal.free();
        frameState.pushOperand(NumberType.I32);

        emitter.loadLocal(valueLocal);
        valueLocal.free();
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
        emitter.pop();
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
        emitAccessMemoryField(i, false, context);

        frameState.popOperand(ReferenceType.OBJECT);
        frameState.popOperand(NumberType.I32);
        emitter.op(Op.SWAP); // Swap the memory with the offset
        frameState.pushOperand(ReferenceType.OBJECT);
        frameState.pushOperand(NumberType.I32);

        emitter.invoke(
                ObjectType.of(ByteBuffer.class),
                methodName,
                new JavaType[]{PrimitiveType.INT},
                returnType,
                InvokeType.VIRTUAL,
                false
        );
        frameState.popOperand(NumberType.I32);
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

    @Override
    public void emitMemoryGrow(LargeArrayIndex i, MemoryType type, CodeEmitContext context) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        emitMemorySize(i, type, context);

        // Duplicate the old size 2 slots down
        // NOTE: theoretically this would need to be a pop, pop, push, push, push, but since all of the types are I32,
        // this is effectively equivalent to just pushing a new I32
        frameState.pushOperand(NumberType.I32);
        emitter.duplicate(1, 1);

        // Calculate new page count
        emitter.op(Op.IADD);
        frameState.popOperand(NumberType.I32);

        Integer maxPageCount = type.getLimits().getMax();
        CodeLabel endLabel = null;

        if (maxPageCount != null) {
            // And test if valid, returning -1 if not
            CodeLabel okLabel = emitter.newLabel();
            endLabel = emitter.newLabel();

            frameState.pushOperand(NumberType.I32);
            frameState.pushOperand(NumberType.I32);
            emitter.duplicate();
            emitter.loadConstant(maxPageCount);

            // Check if new is in bounds - if so, jump to the end
            emitter.jump(JumpCondition.INT_LESS_THAN_OR_EQUAL, okLabel);
            frameState.popOperand(NumberType.I32);
            frameState.popOperand(NumberType.I32);

            // Not in bounds, clear the new and old size from the stack,
            // and load -1
            emitter.pop();
            emitter.pop();

            emitter.loadConstant(-1);
            emitter.jump(JumpCondition.ALWAYS, endLabel);

            emitter.resolveLabel(okLabel);
        }

        // Allocate a new buffer...
        frameState.pushOperand(NumberType.I32);
        emitter.loadConstant(PAGE_SIZE);
        emitter.op(Op.IMUL);
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(NumberType.I32);
        emitter.invoke(
                MEMORY_TYPE,
                "allocateDirect",
                new JavaType[]{ PrimitiveType.INT },
                MEMORY_TYPE,
                InvokeType.STATIC,
                false
        );
        frameState.pushOperand(ReferenceType.OBJECT);

        // Ensure order is correct, load the other buffer into it and replace
        emitEnforceByteOrder(context);
        emitAccessMemoryField(i, false, context);
        emitter.invoke(
                MEMORY_TYPE,
                "put",
                new JavaType[] { MEMORY_TYPE },
                MEMORY_TYPE,
                InvokeType.VIRTUAL,
                false
        );
        frameState.popOperand(ReferenceType.OBJECT);

        emitAccessMemoryField(i, true, context);

        // Now the old size on top of the stack, as it should be!
        if (endLabel != null) {
            emitter.resolveLabel(endLabel);
        }
    }

    @Override
    public void emitMemorySize(LargeArrayIndex i, MemoryType type, CodeEmitContext context) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        emitLoadMemoryReference(i, context);

        emitter.invoke(
                MEMORY_TYPE,
                "limit",
                new JavaType[0],
                PrimitiveType.INT,
                InvokeType.VIRTUAL,
                false
        );

        frameState.popOperand(ReferenceType.OBJECT);
        frameState.pushOperand(NumberType.I32);

        frameState.pushOperand(NumberType.I32);
        emitter.loadConstant(PAGE_SIZE);
        emitter.op(Op.IDIV);
        frameState.popOperand(NumberType.I32);
    }

    @Override
    public void emitMemoryCopy(ObjectType sourceMemoryType, ObjectType targetMemoryType, CodeEmitContext context) throws WasmAssemblerException {
        /*
         * - source memory reference
         * - count
         * - source start index
         * - destination start index
         * - destination memory reference
         */
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        // Stow everything away in locals
        JavaLocal sourceMemoryLocal = emitter.allocateLocal(sourceMemoryType);
        emitter.storeLocal(sourceMemoryLocal);
        frameState.popOperand(ReferenceType.OBJECT);

        JavaLocal countLocal = emitter.allocateLocal(PrimitiveType.INT);
        emitter.storeLocal(countLocal);
        frameState.popOperand(NumberType.I32);

        JavaLocal sourceStartLocal = emitter.allocateLocal(PrimitiveType.INT);
        emitter.storeLocal(sourceStartLocal);
        frameState.popOperand(NumberType.I32);

        JavaLocal destinationStartLocal = emitter.allocateLocal(PrimitiveType.INT);
        emitter.storeLocal(destinationStartLocal);
        frameState.popOperand(NumberType.I32);

        JavaLocal destinationMemoryLocal = emitter.allocateLocal(targetMemoryType);
        emitter.storeLocal(destinationMemoryLocal);
        frameState.popOperand(ReferenceType.OBJECT);

        if (sourceMemoryType.equals(BYTE_ARRAY_TYPE) && targetMemoryType.equals(BYTE_ARRAY_TYPE)) {
            // System.arraycopy is the most efficient way to copy byte arrays
            frameState.pushOperand(ReferenceType.OBJECT);
            emitter.loadLocal(sourceMemoryLocal);

            frameState.pushOperand(NumberType.I32);
            emitter.loadLocal(sourceStartLocal);

            frameState.pushOperand(ReferenceType.OBJECT);
            emitter.loadLocal(destinationMemoryLocal);

            frameState.pushOperand(NumberType.I32);
            emitter.loadLocal(destinationStartLocal);

            frameState.pushOperand(NumberType.I32);
            emitter.loadLocal(countLocal);

            emitter.invoke(
                    ObjectType.of(System.class),
                    "arraycopy",
                    new JavaType[]{
                            BYTE_ARRAY_TYPE,
                            PrimitiveType.INT,
                            BYTE_ARRAY_TYPE,
                            PrimitiveType.INT,
                            PrimitiveType.INT
                    },
                    PrimitiveType.VOID,
                    InvokeType.STATIC,
                    false
            );

            frameState.popOperand(NumberType.I32);
            frameState.popOperand(NumberType.I32);
            frameState.popOperand(ReferenceType.OBJECT);
            frameState.popOperand(NumberType.I32);
            frameState.popOperand(ReferenceType.OBJECT);
        } else if (targetMemoryType.equals(MEMORY_TYPE)) {
            // Use the ByteBuffer API to copy the memory
            frameState.pushOperand(ReferenceType.OBJECT);
            emitter.loadLocal(destinationMemoryLocal);

            frameState.pushOperand(NumberType.I32);
            emitter.loadLocal(destinationStartLocal);

            frameState.pushOperand(ReferenceType.OBJECT);
            emitter.loadLocal(sourceMemoryLocal);

            frameState.pushOperand(NumberType.I32);
            emitter.loadLocal(sourceStartLocal);

            frameState.pushOperand(NumberType.I32);
            emitter.loadLocal(countLocal);

            emitter.invoke(
                    MEMORY_TYPE,
                    "put",
                    new JavaType[]{PrimitiveType.INT, targetMemoryType, PrimitiveType.INT, PrimitiveType.INT},
                    MEMORY_TYPE,
                    InvokeType.VIRTUAL,
                    false
            );

            frameState.popOperand(NumberType.I32);
            frameState.popOperand(NumberType.I32);
            frameState.popOperand(ReferenceType.OBJECT);
            frameState.popOperand(NumberType.I32);
            frameState.popOperand(ReferenceType.OBJECT);
        } else if (targetMemoryType.equals(BYTE_ARRAY_TYPE) && sourceMemoryType.equals(MEMORY_TYPE)) {
            // Use the ByteBuffer API to copy the memory to a byte array
            frameState.pushOperand(ReferenceType.OBJECT);
            emitter.loadLocal(sourceMemoryLocal);

            frameState.pushOperand(NumberType.I32);
            emitter.loadLocal(sourceStartLocal);

            frameState.pushOperand(ReferenceType.OBJECT);
            emitter.loadLocal(destinationMemoryLocal);

            frameState.pushOperand(NumberType.I32);
            emitter.loadLocal(destinationStartLocal);

            frameState.pushOperand(NumberType.I32);
            emitter.loadLocal(countLocal);

            emitter.invoke(
                    MEMORY_TYPE,
                    "get",
                    new JavaType[]{PrimitiveType.INT, BYTE_ARRAY_TYPE, PrimitiveType.INT, PrimitiveType.INT},
                    MEMORY_TYPE,
                    InvokeType.VIRTUAL,
                    false
            );

            frameState.popOperand(NumberType.I32);
            frameState.popOperand(NumberType.I32);
            frameState.popOperand(ReferenceType.OBJECT);
            frameState.popOperand(NumberType.I32);
            frameState.popOperand(ReferenceType.OBJECT);
        } else {
            throw new WasmAssemblerException("Memory copy not supported for types " + sourceMemoryType + " and " + targetMemoryType);
        }

        // Free the locals
        sourceMemoryLocal.free();
        sourceStartLocal.free();
        destinationMemoryLocal.free();
        destinationStartLocal.free();
        countLocal.free();
    }

    @Override
    public void emitLoadData(LargeArrayIndex i, DataSegment segment, CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();
        WasmFrameState frameState = context.getFrameState();

        frameState.pushOperand(ReferenceType.OBJECT);
        emitter.loadThis();
        emitter.accessField(
                context.getEmitter().getOwner(),
                generateDataSegmentFieldName(i),
                DATA_SEGMENT_TYPE,
                false,
                false
        );
        frameState.popOperand(ReferenceType.OBJECT);
        frameState.popOperand(NumberType.I32);
        emitter.op(Op.SWAP);
        frameState.pushOperand(ReferenceType.OBJECT);
        frameState.pushOperand(NumberType.I32);

        emitter.loadArrayElement();

        frameState.popOperand(NumberType.I32);
        frameState.popOperand(ReferenceType.OBJECT);

        frameState.pushOperand(NumberType.I32);
    }

    @Override
    public void emitDropData(LargeArrayIndex i, DataSegment segment, CodeEmitContext context) {
        // This is a no-op in the default implementation, as we don't need to do anything special
    }

    @Override
    public void emitLoadMemoryReference(LargeArrayIndex i, CodeEmitContext context) throws WasmAssemblerException {
        emitAccessMemoryField(i, false, context);
    }

    @Override
    public ObjectType getMemoryType(LargeArrayIndex i) {
        return MEMORY_TYPE;
    }

    @Override
    public boolean canEmitInitFor(ObjectType memoryType) {
        return memoryType.equals(MEMORY_TYPE);
    }

    @Override
    public boolean canEmitCopyFor(ObjectType from, ObjectType to) {
        return (from.equals(MEMORY_TYPE) || from.equals(BYTE_ARRAY_TYPE)) && (to.equals(MEMORY_TYPE) || to.equals(BYTE_ARRAY_TYPE));
    }

    protected String generateMemoryFieldName(LargeArrayIndex i) {
        if (fieldName != null) {
            if (i != null) {
                throw new IllegalArgumentException("Memory index must not be specified when a field name is provided");
            }

            return fieldName;
        }

        if (i == null) {
            throw new IllegalArgumentException("Memory index most be specified");
        }

        return "memory_" + i;
    }

    protected String generateDataSegmentFieldName(LargeArrayIndex i) {
        if (i == null) {
            throw new IllegalArgumentException("Memory index must be specified");
        }

        return "data_" + i;
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

    private void emitAccessMemoryField(
            LargeArrayIndex i,
            boolean isSet,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        frameState.pushOperand(ReferenceType.OBJECT);
        emitter.loadThis();

        if (isSet) {
            // Make sure to get the value to set to the top
            frameState.popOperand(ReferenceType.OBJECT);
            frameState.popOperand(ReferenceType.OBJECT);
            emitter.op(Op.SWAP);
            frameState.pushOperand(ReferenceType.OBJECT);
            frameState.pushOperand(ReferenceType.OBJECT);
        }

        emitter.accessField(
                context.getEmitter().getOwner(),
                generateMemoryFieldName(i),
                MEMORY_TYPE,
                false,
                isSet
        );

        if (isSet) {
            // Setting consumes both the this reference and the object to set
            frameState.popOperand(ReferenceType.OBJECT);
            frameState.popOperand(ReferenceType.OBJECT);
        }
    }

    private void emitEnforceByteOrder(
            CodeEmitContext context
    ) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

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
    }
}
