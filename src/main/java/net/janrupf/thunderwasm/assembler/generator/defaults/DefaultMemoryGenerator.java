package net.janrupf.thunderwasm.assembler.generator.defaults;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.*;
import net.janrupf.thunderwasm.assembler.generator.MemoryGenerator;
import net.janrupf.thunderwasm.data.Limits;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemory;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryStore;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.encoding.LargeByteArray;
import net.janrupf.thunderwasm.module.section.segment.DataSegment;
import net.janrupf.thunderwasm.runtime.linker.memory.LinkedMemory;
import net.janrupf.thunderwasm.types.*;

import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class DefaultMemoryGenerator implements MemoryGenerator {
    private static final ArrayType BYTE_ARRAY_TYPE = new ArrayType(PrimitiveType.BYTE);
    private static final ArrayType DATA_SEGMENT_TYPE = BYTE_ARRAY_TYPE;
    private static final ObjectType MEMORY_TYPE = ObjectType.of(ByteBuffer.class);
    private static final int PAGE_SIZE = 64 * 1024;
    private static final ObjectType LINKED_MEMORY_HANDLE_TYPE = ObjectType.of(LinkedMemory.Handle.class);

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
    public void addDataSegment(LargeArrayIndex i, DataSegment segment, ClassFileEmitter emitter)
            throws WasmAssemblerException {
        emitter.field(
                generateDataSegmentFieldName(i),
                Visibility.PRIVATE,
                false,
                true,
                DATA_SEGMENT_TYPE,
                null
        );

        MethodEmitter initMethodEmitter = emitter.method(
                generateDataSegmentHelperName(i, "init"),
                Visibility.PRIVATE,
                true,
                false,
                new ArrayType(PrimitiveType.BYTE),
                Collections.emptyList(),
                Collections.emptyList()
        );

        CodeEmitter codeEmitter = initMethodEmitter.code();

        byte[] data = segment.getInit().asFlatArray();

        if (data == null) {
            throw new WasmAssemblerException("Data segment is too large: " + segment.getInit().length());
        }

        JavaLocal dataLocal = codeEmitter.allocateLocal(new ArrayType(PrimitiveType.BYTE));

        // Create a new byte array for the data segment
        codeEmitter.loadConstant(data.length);
        codeEmitter.doNew(DATA_SEGMENT_TYPE);
        codeEmitter.storeLocal(dataLocal);

        int chunkOffset = 0;
        while (chunkOffset < data.length) {
            int chunkSize = Math.min(16000, data.length - chunkOffset);

            String stringifiedChunk = new String(data, chunkOffset, chunkSize, StandardCharsets.ISO_8859_1);

            // Load the chunk and copy it into the array
            codeEmitter.loadConstant(stringifiedChunk);
            codeEmitter.accessField(
                    ObjectType.of(StandardCharsets.class),
                    "ISO_8859_1",
                    ObjectType.of(Charset.class),
                    true,
                    false
            );
            codeEmitter.invoke(
                    ObjectType.of(String.class),
                    "getBytes",
                    new JavaType[] { ObjectType.of(Charset.class) },
                    new ArrayType(PrimitiveType.BYTE),
                    InvokeType.VIRTUAL,
                    false
            );

            // And array-copy it into the appropriate locatin...
            codeEmitter.loadConstant(0);
            codeEmitter.loadLocal(dataLocal);
            codeEmitter.loadConstant(chunkOffset);
            codeEmitter.loadConstant(chunkSize);
            codeEmitter.invoke(
                    ObjectType.of(System.class),
                    "arraycopy",
                    new JavaType[] { ObjectType.OBJECT, PrimitiveType.INT, ObjectType.OBJECT, PrimitiveType.INT, PrimitiveType.INT },
                    PrimitiveType.VOID,
                    InvokeType.STATIC,
                    false
            );


            chunkOffset += chunkSize;
        }

        codeEmitter.loadLocal(dataLocal);
        codeEmitter.doReturn();

        codeEmitter.finish();
        initMethodEmitter.finish();
    }

    @Override
    public void emitMemoryConstructor(LargeArrayIndex i, MemoryType type, CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();
        Limits limits = type.getLimits();


        emitter.loadConstant(limits.getMin() * PAGE_SIZE);
        emitter.invoke(
                MEMORY_TYPE,
                "allocateDirect",
                new JavaType[]{PrimitiveType.INT},
                MEMORY_TYPE,
                InvokeType.STATIC,
                false
        );


        emitEnforceByteOrder(context);
        emitAccessMemoryField(i, true, context);
    }

    @Override
    public void emitDataSegmentConstructor(LargeArrayIndex i, DataSegment segment, CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();
        LargeByteArray data = segment.getInit();

        if (data.length() > Integer.MAX_VALUE) {
            throw new WasmAssemblerException("Data segment is too large: " + data.length());
        }

        emitter.loadLocal(context.getLocalVariables().getThis());

        emitter.invoke(
                emitter.getOwner(),
                generateDataSegmentHelperName(i, "init"),
                new JavaType[0],
                new ArrayType(PrimitiveType.BYTE),
                InvokeType.STATIC,
                false
        );

        emitter.accessField(
                context.getEmitter().getOwner(),
                generateDataSegmentFieldName(i),
                DATA_SEGMENT_TYPE,
                false,
                true
        );

    }

    @Override
    public void emitMemoryInit(
            LargeArrayIndex memoryIndex,
            MemoryType type,
            LargeArrayIndex dataIndex,
            DataSegment segment,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        /* This method expects the following stack top:
         * - count
         * - source start index
         * - destination start index
         */

        JavaLocal countLocal = emitter.allocateLocal(PrimitiveType.INT);
        emitter.storeLocal(countLocal);

        JavaLocal sourceStartLocal = emitter.allocateLocal(PrimitiveType.INT);
        emitter.storeLocal(sourceStartLocal);

        JavaLocal destinationStartLocal = emitter.allocateLocal(PrimitiveType.INT);
        emitter.storeLocal(destinationStartLocal);

        emitter.loadLocal(context.getLocalVariables().getThis());

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

        emitter.loadLocal(destinationStartLocal);

        emitter.op(Op.SWAP);

        emitter.loadLocal(sourceStartLocal);

        emitter.loadLocal(countLocal);

        emitter.invoke(
                MEMORY_TYPE,
                "put",
                new JavaType[]{PrimitiveType.INT, DATA_SEGMENT_TYPE, PrimitiveType.INT, PrimitiveType.INT},
                MEMORY_TYPE,
                InvokeType.VIRTUAL,
                false
        );


        emitter.pop();

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

        emitCalculateAccessOffset(memarg, context);

        JavaLocal offsetLocal = emitter.allocateLocal(PrimitiveType.INT);
        emitter.storeLocal(offsetLocal);

        emitAccessMemoryField(i, false, context);

        emitter.loadLocal(offsetLocal);
        offsetLocal.free();

        emitter.loadLocal(valueLocal);
        valueLocal.free();

        emitter.invoke(
                ObjectType.of(ByteBuffer.class),
                methodName,
                new JavaType[]{PrimitiveType.INT, argumentType},
                ObjectType.of(ByteBuffer.class),
                InvokeType.VIRTUAL,
                false
        );
        emitter.pop();

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
        CodeEmitter emitter = context.getEmitter();
        JavaType javaType = WasmTypeConverter.toJavaType(numberType);

        String methodName;
        JavaType returnType;
        int bitWidth;

        if (numberType == NumberType.I32 || numberType == NumberType.I64) {
            switch (loadType) {
                case SIGNED_8:
                case UNSIGNED_8:
                    returnType = PrimitiveType.BYTE;
                    methodName = "get";
                    bitWidth = 8;
                    break;

                case SIGNED_16:
                case UNSIGNED_16:
                    returnType = PrimitiveType.SHORT;
                    methodName = "getShort";
                    bitWidth = 16;
                    break;

                case SIGNED_32:
                case UNSIGNED_32:
                    returnType = PrimitiveType.INT;
                    methodName = "getInt";
                    bitWidth = 32;
                    break;

                case NATIVE:
                    returnType = javaType;

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

        emitter.op(Op.SWAP); // Swap the memory with the offset

        emitter.invoke(
                ObjectType.of(ByteBuffer.class),
                methodName,
                new JavaType[]{PrimitiveType.INT},
                returnType,
                InvokeType.VIRTUAL,
                false
        );

        if (!javaType.equals(returnType)) {
            // Need to convert the value to the correct type
            if (javaType.equals(PrimitiveType.LONG)) {
                // Upcast to long, this is always required when we need to upcast to something larger
                emitter.op(Op.I2L);
            }

            if (!loadType.isSigned()) {
                // Unsigned load, we need to mask away the upper bits
                if (numberType == NumberType.I32) {
                    emitter.loadConstant((1 << bitWidth) - 1);
                    emitter.op(Op.IAND);
                } else { // I64
                    emitter.loadConstant((1L << bitWidth) - 1);
                    emitter.op(Op.LAND);
                }
            }
        }
    }

    @Override
    public void emitMemoryGrow(LargeArrayIndex i, MemoryType type, CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        emitMemorySize(i, type, context);

        // Duplicate the old size 2 slots down
        // NOTE: theoretically this would need to be a pop, pop, push, push, push, but since all of the types are I32,
        // this is effectively equivalent to just pushing a new I32
        emitter.duplicate(1, 1);

        // Calculate new page count
        emitter.op(Op.IADD);

        Integer maxPageCount = type.getLimits().getMax();
        CodeLabel endLabel = null;

        if (maxPageCount != null) {
            // And test if valid, returning -1 if not
            CodeLabel okLabel = emitter.newLabel();
            endLabel = emitter.newLabel();

            emitter.duplicate();
            emitter.loadConstant(maxPageCount);

            // Check if new is in bounds - if so, jump to the end
            emitter.jump(JumpCondition.INT_LESS_THAN_OR_EQUAL, okLabel);

            // Not in bounds, clear the new and old size from the stack,
            // and load -1
            emitter.pop();
            emitter.pop();

            emitter.loadConstant(-1);
            emitter.jump(JumpCondition.ALWAYS, endLabel);

            emitter.resolveLabel(okLabel);
        }

        // Allocate a new buffer...
        emitter.loadConstant(PAGE_SIZE);
        emitter.op(Op.IMUL);
        emitter.invoke(
                MEMORY_TYPE,
                "allocateDirect",
                new JavaType[]{ PrimitiveType.INT },
                MEMORY_TYPE,
                InvokeType.STATIC,
                false
        );

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
        emitter.invoke(
                MEMORY_TYPE,
                "clear",
                new JavaType[] {},
                MEMORY_TYPE,
                InvokeType.VIRTUAL,
                false
        );

        emitAccessMemoryField(i, true, context);

        // Now the old size on top of the stack, as it should be!
        if (endLabel != null) {
            emitter.resolveLabel(endLabel);
        }
    }

    @Override
    public void emitMemorySize(LargeArrayIndex i, MemoryType type, CodeEmitContext context) throws WasmAssemblerException {
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


        emitter.loadConstant(PAGE_SIZE);
        emitter.op(Op.IDIV);
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
        CodeEmitter emitter = context.getEmitter();

        // Stow everything away in locals
        JavaLocal sourceMemoryLocal = emitter.allocateLocal(sourceMemoryType);
        emitter.storeLocal(sourceMemoryLocal);

        JavaLocal countLocal = emitter.allocateLocal(PrimitiveType.INT);
        emitter.storeLocal(countLocal);

        JavaLocal sourceStartLocal = emitter.allocateLocal(PrimitiveType.INT);
        emitter.storeLocal(sourceStartLocal);

        JavaLocal destinationStartLocal = emitter.allocateLocal(PrimitiveType.INT);
        emitter.storeLocal(destinationStartLocal);

        JavaLocal destinationMemoryLocal = emitter.allocateLocal(targetMemoryType);
        emitter.storeLocal(destinationMemoryLocal);

        if (sourceMemoryType.equals(BYTE_ARRAY_TYPE) && targetMemoryType.equals(BYTE_ARRAY_TYPE)) {
            // System.arraycopy is the most efficient way to copy byte arrays
            emitter.loadLocal(sourceMemoryLocal);

            emitter.loadLocal(sourceStartLocal);

            emitter.loadLocal(destinationMemoryLocal);

            emitter.loadLocal(destinationStartLocal);

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

        } else if (targetMemoryType.equals(MEMORY_TYPE)) {
            // Use the ByteBuffer API to copy the memory
            emitter.loadLocal(destinationMemoryLocal);

            emitter.loadLocal(destinationStartLocal);

            emitter.loadLocal(sourceMemoryLocal);

            emitter.loadLocal(sourceStartLocal);

            emitter.loadLocal(countLocal);

            emitter.invoke(
                    MEMORY_TYPE,
                    "put",
                    new JavaType[]{PrimitiveType.INT, targetMemoryType, PrimitiveType.INT, PrimitiveType.INT},
                    MEMORY_TYPE,
                    InvokeType.VIRTUAL,
                    false
            );

            emitter.pop();

        } else if (targetMemoryType.equals(BYTE_ARRAY_TYPE) && sourceMemoryType.equals(MEMORY_TYPE)) {
            // Use the ByteBuffer API to copy the memory to a byte array
            emitter.loadLocal(sourceMemoryLocal);

            emitter.loadLocal(sourceStartLocal);

            emitter.loadLocal(destinationMemoryLocal);

            emitter.loadLocal(destinationStartLocal);

            emitter.loadLocal(countLocal);

            emitter.invoke(
                    MEMORY_TYPE,
                    "get",
                    new JavaType[]{PrimitiveType.INT, BYTE_ARRAY_TYPE, PrimitiveType.INT, PrimitiveType.INT},
                    MEMORY_TYPE,
                    InvokeType.VIRTUAL,
                    false
            );

            emitter.pop();
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

        emitter.loadLocal(context.getLocalVariables().getThis());
        emitter.accessField(
                context.getEmitter().getOwner(),
                generateDataSegmentFieldName(i),
                DATA_SEGMENT_TYPE,
                false,
                false
        );
        emitter.op(Op.SWAP);

        emitter.loadArrayElement();
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
    public void makeMemoryExportable(LargeArrayIndex i, MemoryType type, ClassEmitContext context) throws WasmAssemblerException {
        // We need to add a method for growing

        MethodEmitter methodEmitter = context.getEmitter().method(
                generateMemoryHelperName(i, "grow"),
                Visibility.PRIVATE,
                false,
                false,
                PrimitiveType.BOOLEAN,
                Collections.singletonList(PrimitiveType.INT),
                Collections.emptyList()
        );

        CodeEmitter growCodeEmitter = methodEmitter.code();
        JavaLocal thisLocal = methodEmitter.getThisLocal();
        JavaLocal growCountLocal = methodEmitter.getArgumentLocals().get(0);

        growCodeEmitter.loadLocal(growCountLocal);
        emitMemoryGrow(i, type, new CodeEmitContext(
                generateMemoryHelperName(i, "grow") + "$block$",
                null,
                context.getEmitter(),
                growCodeEmitter,
                context.getLookups(),
                new WasmFrameState(),
                context.getGenerators(),
                new LocalVariables(thisLocal, null, null),
                context.getConfiguration()
        ));

        // Returns the old size or -1 on failure - we need to convert
        // everything except -1 to a true
        growCodeEmitter.loadConstant(1);
        growCodeEmitter.op(Op.IADD);
        growCodeEmitter.duplicate();
        growCodeEmitter.op(Op.INEG);
        growCodeEmitter.op(Op.IOR);

        growCodeEmitter.loadConstant(31);
        growCodeEmitter.op(Op.IUSHR);

        growCodeEmitter.doReturn();
        growCodeEmitter.finish();
        methodEmitter.finish();
    }

    @Override
    public void emitLoadMemoryExport(LargeArrayIndex i, MemoryType type, CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        emitter.doNew(LINKED_MEMORY_HANDLE_TYPE);
        emitter.duplicate();

        emitter.loadConstant(new JavaFieldHandle(
                emitter.getOwner(),
                generateMemoryFieldName(i),
                MEMORY_TYPE,
                false,
                false,
                false
        ));
        emitter.loadLocal(context.getLocalVariables().getThis());
        CommonBytecodeGenerator.bindMethodHandle(emitter);

        emitter.loadConstant(new JavaMethodHandle(
                emitter.getOwner(),
                generateMemoryHelperName(i, "grow"),
                PrimitiveType.BOOLEAN,
                Collections.singletonList(PrimitiveType.INT),
                InvokeType.VIRTUAL,
                false
        ));
        emitter.loadLocal(context.getLocalVariables().getThis());
        CommonBytecodeGenerator.bindMethodHandle(emitter);

        emitter.invoke(
                LINKED_MEMORY_HANDLE_TYPE,
                "<init>",
                new JavaType[] { ObjectType.of(MethodHandle.class), ObjectType.of(MethodHandle.class) },
                PrimitiveType.VOID,
                InvokeType.SPECIAL,
                false
        );
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

    protected String generateDataSegmentHelperName(LargeArrayIndex i, String purpose) {
        return "$data_" + i + "$" + purpose;
    }

    protected String generateMemoryHelperName(LargeArrayIndex i, String purpose) {
        return "$memory_" + i + "$" + purpose;
    }

    private void emitCalculateAccessOffset(
            PlainMemory.Memarg memarg,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        if (memarg.getOffset() == 0) {
            // Nothing further to do
            return;
        }

        emitter.loadConstant(memarg.getOffset());
        emitter.op(Op.IADD);
    }

    private void emitAccessMemoryField(
            LargeArrayIndex i,
            boolean isSet,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        emitter.loadLocal(context.getLocalVariables().getThis());

        if (isSet) {
            // Make sure to get the value to set to the top
            emitter.op(Op.SWAP);
        }

        emitter.accessField(
                context.getEmitter().getOwner(),
                generateMemoryFieldName(i),
                MEMORY_TYPE,
                false,
                isSet
        );
    }

    private void emitEnforceByteOrder(
            CodeEmitContext context
    ) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

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
    }
}
