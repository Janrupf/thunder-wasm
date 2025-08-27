package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.assembler.generator.MemoryGenerator;
import net.janrupf.thunderwasm.imports.MemoryImportDescription;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.WasmU32VariantInstruction;
import net.janrupf.thunderwasm.instructions.data.DoubleIndexData;
import net.janrupf.thunderwasm.instructions.data.MemoryIndexData;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.runtime.BoundsChecks;
import net.janrupf.thunderwasm.types.MemoryType;
import net.janrupf.thunderwasm.types.NumberType;

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
    public ProcessedInstruction processInputs(CodeEmitContext context, DoubleIndexData<MemoryIndexData, MemoryIndexData> data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I32); // n (count)
        context.getFrameState().popOperand(NumberType.I32); // s (source start index)
        context.getFrameState().popOperand(NumberType.I32); // d (destination start index)

        final MemoryIndexData target = data.getFirst();
        final MemoryIndexData source = data.getSecond();

        final LargeArrayIndex sourceIndex = source.toArrayIndex();
        final FoundElement<MemoryType, MemoryImportDescription> sourceElement = context.getLookups().requireMemory(sourceIndex);

        final LargeArrayIndex targetIndex = target.toArrayIndex();
        final FoundElement<MemoryType, MemoryImportDescription> targetElement = context.getLookups().requireMemory(targetIndex);

        return new ProcessedInstruction() {
            @Override
            public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
                CodeEmitter emitter = context.getEmitter();
                MemoryGenerator memoryGenerator = context.getGenerators().getMemoryGenerator();

                MemoryInstructionHelper sourceHelper = new MemoryInstructionHelper(sourceElement, context);
                MemoryInstructionHelper targetHelper = new MemoryInstructionHelper(targetElement, context);

                if (context.getConfiguration().atomicBoundsChecksEnabled()) {
                    CommonBytecodeGenerator.emitPrepareCopyBoundsCheck(emitter);
                    sourceHelper.emitMemorySize();
                    targetHelper.emitMemorySize();

                    emitter.invoke(
                            ObjectType.of(BoundsChecks.class),
                            "checkMemoryCopyBulkAccess",
                            new JavaType[]{PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT},
                            PrimitiveType.VOID,
                            InvokeType.STATIC,
                            false
                    );
                }

                // If the internal generator can emit a copy for the given types, use it
                if (memoryGenerator.canEmitCopyFor(sourceHelper.getJavaMemoryType(), targetHelper.getJavaMemoryType())) {

                    CommonBytecodeGenerator.loadBelow(
                            emitter,
                            3,
                            targetHelper.getJavaMemoryType(),
                            targetHelper::emitLoadMemoryReference
                    );

                    sourceHelper.emitLoadMemoryReference();

                    memoryGenerator.emitMemoryCopy(sourceHelper.getJavaMemoryType(), targetHelper.getJavaMemoryType(), context);
                    return;
                }

                emitFallbackCopy(sourceHelper, targetHelper, context);
            }
            
            @Override
            public void processOutputs(CodeEmitContext context) {
            }
        };
    }

    private void emitFallbackCopy(
            MemoryInstructionHelper sourceHelper,
            MemoryInstructionHelper targetHelper,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        // Stack top:
        // - n (count)
        // - s (source start index)
        // - d (destination start index)

        // Store n, s and d in locals
        JavaLocal nLocal = emitter.allocateLocal(PrimitiveType.INT);
        JavaLocal sLocal = emitter.allocateLocal(PrimitiveType.INT);
        JavaLocal dLocal = emitter.allocateLocal(PrimitiveType.INT);

        emitter.storeLocal(nLocal);
        emitter.storeLocal(sLocal);
        emitter.storeLocal(dLocal);

        CodeLabel endLabel = emitter.newLabel();

        // Check if d <= s
        emitter.loadLocal(sLocal);
        emitter.loadLocal(dLocal);

        // Jump if d > s
        CodeLabel dGreaterThanS = emitter.newLabel();
        emitter.jump(JumpCondition.INT_GREATER_THAN, dGreaterThanS);

        // If d <= s, copy from the start
        CodeLabel dLessEqLoopStart = emitter.newLabel();
        emitter.resolveLabel(dLessEqLoopStart);

        // Jump to end if n == 0
        emitter.loadLocal(nLocal);
        emitter.jump(JumpCondition.INT_EQUAL_ZERO, endLabel);

        emitter.loadLocal(dLocal);
        emitter.loadLocal(sLocal);
        sourceHelper.emitMemoryLoadByte();
        targetHelper.emitMemoryStoreByte();

        // Increment and store s
        emitter.loadLocal(sLocal);
        emitter.loadConstant(1);
        emitter.op(Op.IADD);
        emitter.storeLocal(sLocal);

        // Increment and store d
        emitter.loadLocal(dLocal);
        emitter.loadConstant(1);
        emitter.op(Op.IADD);
        emitter.storeLocal(dLocal);

        // Decrement n
        emitter.loadLocal(nLocal);
        emitter.loadConstant(1);
        emitter.op(Op.ISUB);
        emitter.storeLocal(nLocal);

        // Jump to end if zero
        emitter.jump(JumpCondition.ALWAYS, dLessEqLoopStart);

        // If d > s, copy from the end
        emitter.resolveLabel(dGreaterThanS);

        // Jump to end if n == 0
        emitter.loadLocal(nLocal);
        emitter.jump(JumpCondition.INT_EQUAL_ZERO, endLabel);

        CodeLabel dGreaterThanSLoopStart = emitter.newLabel();
        emitter.resolveLabel(dGreaterThanSLoopStart);

        // Push d + n - 1
        emitter.loadLocal(dLocal);
        emitter.loadLocal(nLocal);
        emitter.loadConstant(1);
        emitter.op(Op.ISUB);
        emitter.op(Op.IADD);

        // Push s + n - 1
        emitter.loadLocal(sLocal);
        emitter.loadLocal(nLocal);
        emitter.loadConstant(1);
        emitter.op(Op.ISUB);
        emitter.op(Op.IADD);

        // Copy the value
        sourceHelper.emitMemoryLoadByte();
        targetHelper.emitMemoryStoreByte();

        // Decrement and store n
        emitter.loadLocal(nLocal);
        emitter.loadConstant(1);
        emitter.op(Op.ISUB);
        emitter.storeLocal(nLocal);

        // Jump if n > 0
        emitter.loadLocal(nLocal);
        emitter.jump(JumpCondition.INT_GREATER_THAN_ZERO, dGreaterThanSLoopStart);

        // Free the locals
        nLocal.free();
        sLocal.free();
        dLocal.free();

        emitter.resolveLabel(endLabel);
    }
}
