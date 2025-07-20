package net.janrupf.thunderwasm.assembler.generator.defaults;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.types.ArrayType;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.assembler.generator.MemoryGenerator;
import net.janrupf.thunderwasm.data.Limits;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemory;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryStore;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.MemoryType;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ReferenceType;

public class DefaultMemoryGenerator implements MemoryGenerator {
    private static final ArrayType MEMORY_TYPE = new ArrayType(PrimitiveType.BYTE);
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

        frameState.pushOperand(ReferenceType.OBJECT);
        frameState.pushOperand(NumberType.I32);

        emitter.loadConstant(limits.getMin() * PAGE_SIZE);
        emitter.doNew(MEMORY_TYPE);

        frameState.popOperand(NumberType.I32);

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

        // Step 1: Calculate the real store offset - for this we make sure the offset is on top of the stack
        int valueLocal = -1;
        if (javaType.getSlotCount() > 1) {
            // Lets store it in a local then
            valueLocal = frameState.computeJavaLocalIndex(frameState.allocateLocal(numberType));

            frameState.popOperand(numberType);
        } else {
            // We can keep it in a stack variable, but need to swap

            frameState.popOperand(numberType);
            frameState.popOperand(NumberType.I32);
            emitter.op(Op.SWAP);
            frameState.pushOperand(numberType);
            frameState.pushOperand(NumberType.I32);
        }

        emitCalculateStoreOffset(memarg, context);

        int stepCount = storeStepCount(numberType, storeType);
        for (int step = 0; step < stepCount; step++) {
            emitByteStore(i, numberType, step, stepCount, valueLocal, context);
        }

        if (valueLocal != -1) {
            frameState.freeLocal();
        }
    }

    protected String generateMemoryFieldName(LargeArrayIndex i) {
        if (i == null) {
            throw new IllegalArgumentException("Memory index most be specified");
        }

        return "memory_" + i;
    }

    private void emitCalculateStoreOffset(
            PlainMemory.Memarg memarg,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        // TODO: Should we do this? It doesn't seem to help the JIT by hinting alignment, though
        //       I only did rough tests. Evaluate more
        // frameState.pushOperand(NumberType.I32);
        // // Mask away the lower bits of the base offset
        // emitter.loadConstant(-(1 << memarg.getAlignment()));
        // emitter.op(Op.IAND);
        // frameState.popOperand(NumberType.I32);

        if (memarg.getOffset() == 0) {
            // Nothing further to do
            return;
        }


        frameState.pushOperand(NumberType.I32);
        emitter.loadConstant(memarg.getOffset());
        emitter.op(Op.IADD);
        frameState.popOperand(NumberType.I32);
    }

    private void emitByteStore(
            LargeArrayIndex i,
            NumberType numberType,
            int step,
            int totalStepCount,
            int valueLocal,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        // Base offset is on top of the stack

        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();
        JavaType javaType = WasmTypeConverter.toJavaType(numberType);

        if (step != totalStepCount - 1) {
            // Make sure we operate on a copy of the values

            if (valueLocal == -1) {
                // Value is also on the stack below the offset
                frameState.pushOperand(numberType);
                frameState.pushOperand(NumberType.I32);

                emitter.duplicate2(javaType, PrimitiveType.INT);
            } else {
                // Duplicate the offset only
                frameState.pushOperand(NumberType.I32);
                emitter.duplicate(PrimitiveType.INT);
            }
        }

        // Offset is now on top of the stack, add the static step offset
        if (step != 0) {
            frameState.pushOperand(NumberType.I32);
            emitter.loadConstant(step);
            emitter.op(Op.IADD);
            frameState.popOperand(NumberType.I32);
        }


        // Load the array ref below the offset (and possibly value)
        CommonBytecodeGenerator.loadBelow(
                frameState,
                emitter,
                valueLocal == -1 ? 2 : 1,
                ReferenceType.OBJECT,
                () -> {
                    frameState.pushOperand(ReferenceType.OBJECT);
                    emitter.loadThis();
                    emitter.accessField(
                            emitter.getOwner(),
                            generateMemoryFieldName(i),
                            MEMORY_TYPE,
                            false,
                            false
                    );

                    // loaded byte array will be pushed by generator
                    frameState.popOperand(ReferenceType.OBJECT);
                }
        );

        // Lets get the value to the top now
        if (valueLocal == -1) {
            // Swap the 2 on the top
            frameState.popOperand(NumberType.I32);
            frameState.popOperand(numberType);
            emitter.op(Op.SWAP);
            frameState.pushOperand(NumberType.I32);
            frameState.pushOperand(numberType);
        } else {
            // Just load it from the local
            frameState.pushOperand(numberType);
            emitter.loadLocal(valueLocal, javaType);
        }

        // Stack is now array, index, value, [top]
        if (step != 0) {
            // Shift the value so we are looking at the next 8 bits
            frameState.pushOperand(NumberType.I32);
            emitter.loadConstant(step * 8);

            if (numberType == NumberType.I64) {
                emitter.op(Op.LUSHR);
            } else {
                emitter.op(Op.IUSHR);
            }

            frameState.popOperand(NumberType.I32);
        }

        // Convert value to an int if not already
        if (numberType == NumberType.I64) {
            frameState.popOperand(NumberType.I64);
            emitter.op(Op.L2I);
            frameState.pushOperand(NumberType.I32);
        }

        // And finally: Store!
        emitter.storeArrayElement(MEMORY_TYPE);

        frameState.popOperand(NumberType.I32);
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(ReferenceType.OBJECT);
    }

    private int storeStepCount(NumberType numberType, PlainMemoryStore.StoreType storeType) {
        switch (storeType) {
            case BIT_8:
                return 1;

            case BIT_16:
                return 2;

            case BIT_32:
                return 4;

            default:
                return nativeAccessCount(numberType);
        }
    }

    private int nativeAccessCount(NumberType numberType) {
        if (numberType == NumberType.I32) {
            return 4;
        } else if (numberType == NumberType.I64) {
            return 8;
        } else if (numberType == NumberType.F32) {
            return 4;
        } else if (numberType == NumberType.F64) {
            return 8;
        }

        throw new IllegalArgumentException("Unsupported number type " + numberType);
    }
}
