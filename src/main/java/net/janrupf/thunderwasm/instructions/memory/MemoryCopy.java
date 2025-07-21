package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.assembler.generator.MemoryGenerator;
import net.janrupf.thunderwasm.imports.MemoryImportDescription;
import net.janrupf.thunderwasm.instructions.WasmU32VariantInstruction;
import net.janrupf.thunderwasm.instructions.data.DoubleIndexData;
import net.janrupf.thunderwasm.instructions.data.MemoryIndexData;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.MemoryType;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ReferenceType;

import java.io.IOException;

public final class MemoryCopy extends WasmU32VariantInstruction<DoubleIndexData<MemoryIndexData, MemoryIndexData>> {
    public static final MemoryCopy INSTANCE = new MemoryCopy();

    public MemoryCopy() {
        super("memory.copy", (byte) 0xFC, 10);
    }

    @Override
    public DoubleIndexData<MemoryIndexData, MemoryIndexData> readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return new DoubleIndexData<>(
            MemoryIndexData.read(loader),
            MemoryIndexData.read(loader)
        );
    }

    @Override
    public void emitCode(CodeEmitContext context, DoubleIndexData<MemoryIndexData, MemoryIndexData> data) throws WasmAssemblerException {
        MemoryIndexData target = data.getFirst();
        MemoryIndexData source = data.getSecond();

        LargeArrayIndex sourceIndex = source.toArrayIndex();
        FoundElement<MemoryType, MemoryImportDescription> sourceElement = context.getLookups().requireMemory(sourceIndex);

        LargeArrayIndex targetIndex = target.toArrayIndex();
        FoundElement<MemoryType, MemoryImportDescription> targetElement = context.getLookups().requireMemory(targetIndex);

        MemoryGenerator memoryGenerator = context.getGenerators().getMemoryGenerator();

        // Extract the necessary information
        MemoryInstructionHelper sourceHelper = new MemoryInstructionHelper(sourceElement, context);
        MemoryInstructionHelper targetHelper = new MemoryInstructionHelper(targetElement, context);

        // If the internal generator can emit a copy for the given types, use it
        if (memoryGenerator.canEmitCopyFor(sourceHelper.getJavaMemoryType(), targetHelper.getJavaMemoryType())) {
            WasmFrameState frameState = context.getFrameState();
            CodeEmitter emitter = context.getEmitter();

            CommonBytecodeGenerator.loadBelow(
                    frameState,
                    emitter,
                    3,
                    ReferenceType.OBJECT,
                    () -> {
                        targetHelper.emitLoadMemoryReference();
                        frameState.popOperand(ReferenceType.OBJECT);
                    }
            );

            sourceHelper.emitLoadMemoryReference();

            memoryGenerator.emitMemoryCopy(sourceHelper.getJavaMemoryType(), targetHelper.getJavaMemoryType(), context);
            return;
        }

        // If the internal generator cannot emit a copy, emit a fallback copy
        emitFallbackCopy(sourceHelper, targetHelper, context);
    }

    private void emitFallbackCopy(
            MemoryInstructionHelper sourceHelper,
            MemoryInstructionHelper targetHelper,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        // Stack top:
        // - n (count)
        // - s (source start index)
        // - d (destination start index)

        // Store n, s and d in locals
        int nLocal = frameState.computeJavaLocalIndex(frameState.allocateLocal(NumberType.I32));
        int sLocal = frameState.computeJavaLocalIndex(frameState.allocateLocal(NumberType.I32));
        int dLocal = frameState.computeJavaLocalIndex(frameState.allocateLocal(NumberType.I32));

        emitter.storeLocal(nLocal, PrimitiveType.INT);
        emitter.storeLocal(sLocal, PrimitiveType.INT);
        emitter.storeLocal(dLocal, PrimitiveType.INT);

        frameState.popOperand(NumberType.I32);
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(NumberType.I32);

        CodeLabel endLabel = emitter.newLabel();

        // Check if d <= s
        emitter.loadLocal(sLocal, PrimitiveType.INT);
        emitter.loadLocal(dLocal, PrimitiveType.INT);

        // Jump if d > s
        CodeLabel dGreaterThanS = emitter.newLabel();
        emitter.jump(JumpCondition.INT_GREATER_THAN, dGreaterThanS);

        // If d <= s, copy from the start
        CodeLabel dLessEqLoopStart = emitter.newLabel();
        emitter.resolveLabel(dLessEqLoopStart, frameState.computeSnapshot());

        // Jump to end if n == 0
        emitter.loadLocal(nLocal, PrimitiveType.INT);
        emitter.jump(JumpCondition.INT_EQUAL_ZERO, endLabel);

        // Copy the value
        frameState.pushOperand(NumberType.I32);
        frameState.pushOperand(NumberType.I32);

        emitter.loadLocal(dLocal, PrimitiveType.INT);
        emitter.loadLocal(sLocal, PrimitiveType.INT);
        sourceHelper.emitMemoryLoadByte();
        targetHelper.emitMemoryStoreByte();

        // Increment and store s
        emitter.loadLocal(sLocal, PrimitiveType.INT);
        emitter.loadConstant(1);
        emitter.op(Op.IADD);
        emitter.storeLocal(sLocal, PrimitiveType.INT);

        // Increment and store d
        emitter.loadLocal(dLocal, PrimitiveType.INT);
        emitter.loadConstant(1);
        emitter.op(Op.IADD);
        emitter.storeLocal(dLocal, PrimitiveType.INT);

        // Decrement n
        emitter.loadLocal(nLocal, PrimitiveType.INT);
        emitter.loadConstant(1);
        emitter.op(Op.ISUB);
        emitter.storeLocal(nLocal, PrimitiveType.INT);

        // Jump to end if zero
        emitter.jump(JumpCondition.ALWAYS, dLessEqLoopStart);

        // If d > s, copy from the end
        emitter.resolveLabel(dGreaterThanS, frameState.computeSnapshot());

        // Jump to end if n == 0
        emitter.loadLocal(nLocal, PrimitiveType.INT);
        emitter.jump(JumpCondition.INT_EQUAL_ZERO, endLabel);

        CodeLabel dGreaterThanSLoopStart = emitter.newLabel();
        emitter.resolveLabel(dGreaterThanSLoopStart, frameState.computeSnapshot());

        // Push d + n - 1
        frameState.pushOperand(NumberType.I32);
        emitter.loadLocal(dLocal, PrimitiveType.INT);
        emitter.loadLocal(nLocal, PrimitiveType.INT);
        emitter.loadConstant(1);
        emitter.op(Op.ISUB);
        emitter.op(Op.IADD);

        // Push s + n - 1
        frameState.pushOperand(NumberType.I32);
        emitter.loadLocal(sLocal, PrimitiveType.INT);
        emitter.loadLocal(nLocal, PrimitiveType.INT);
        emitter.loadConstant(1);
        emitter.op(Op.ISUB);
        emitter.op(Op.IADD);

        // Copy the value
        sourceHelper.emitMemoryLoadByte();
        targetHelper.emitMemoryStoreByte();

        // Decrement and store n
        emitter.loadLocal(nLocal, PrimitiveType.INT);
        emitter.loadConstant(1);
        emitter.op(Op.ISUB);
        emitter.storeLocal(nLocal, PrimitiveType.INT);

        // Jump if n > 0
        emitter.loadLocal(nLocal, PrimitiveType.INT);
        emitter.jump(JumpCondition.INT_GREATER_THAN_ZERO, dGreaterThanSLoopStart);

        // Free the locals
        frameState.freeLocal();
        frameState.freeLocal();
        frameState.freeLocal();

        emitter.resolveLabel(endLabel, frameState.computeSnapshot());
    }
}
