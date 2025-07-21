package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.assembler.generator.MemoryGenerator;
import net.janrupf.thunderwasm.imports.MemoryImportDescription;
import net.janrupf.thunderwasm.instructions.WasmU32VariantInstruction;
import net.janrupf.thunderwasm.instructions.data.DataIndexData;
import net.janrupf.thunderwasm.instructions.data.DoubleIndexData;
import net.janrupf.thunderwasm.instructions.data.MemoryIndexData;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.segment.DataSegment;
import net.janrupf.thunderwasm.types.MemoryType;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ReferenceType;

import java.io.IOException;

public final class MemoryInit extends WasmU32VariantInstruction<DoubleIndexData<DataIndexData, MemoryIndexData>> {
    public static final MemoryInit INSTANCE = new MemoryInit();

    private MemoryInit() {
        super("memory.init", (byte) 0xFC, 8);
    }

    @Override
    public DoubleIndexData<DataIndexData, MemoryIndexData> readData(WasmLoader loader)
            throws IOException, InvalidModuleException {
        return new DoubleIndexData<>(
                DataIndexData.read(loader),
                MemoryIndexData.read(loader)
        );
    }

    @Override
    public void emitCode(CodeEmitContext context, DoubleIndexData<DataIndexData, MemoryIndexData> data)
            throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter codeEmitter = context.getEmitter();

        DataIndexData dataSegment = data.getFirst();
        MemoryIndexData memory = data.getSecond();

        FoundElement<MemoryType, MemoryImportDescription> memoryElement = context.getLookups().requireMemory(memory.toArrayIndex());
        DataSegment segment = context.getLookups().requireDataSegment(dataSegment.toArrayIndex());

        MemoryInstructionHelper helper = new MemoryInstructionHelper(memoryElement, context);

        MemoryGenerator internalGenerator = context.getGenerators().getMemoryGenerator();

        if (internalGenerator.canEmitInitFor(helper.getJavaMemoryType())) {
            CommonBytecodeGenerator.loadBelow(
                    frameState,
                    codeEmitter,
                    3,
                    ReferenceType.OBJECT,
                    () -> {
                        if (memoryElement.isImport()) {
                            context.getGenerators().getImportGenerator().emitLoadMemoryReference(
                                    memoryElement.getImport(),
                                    context
                            );
                        } else {
                            internalGenerator.emitLoadMemoryReference(
                                    memory.toArrayIndex(),
                                    context
                            );
                        }

                        frameState.popOperand(ReferenceType.OBJECT);
                    }
            );

            if (memoryElement.isImport()) {
                context.getGenerators().getImportGenerator().emitMemoryInit(
                        memoryElement.getImport(),
                        dataSegment.toArrayIndex(),
                        segment,
                        context
                );
            } else {
                internalGenerator.emitMemoryInit(
                        memory.toArrayIndex(),
                        helper.getMemoryType(),
                        dataSegment.toArrayIndex(),
                        segment,
                        context
                );
            }
            return;
        }

        emitFallbackInit(helper, segment, dataSegment.toArrayIndex(), context);
    }

    private void emitFallbackInit(
            MemoryInstructionHelper helper,
            DataSegment segment,
            LargeArrayIndex dataSegmentIndex,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        // Stack top:
        // - n (count)
        // - s (source start index in data segment)
        // - d (destination start index in memory)

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

        CodeLabel copyLoopStart = emitter.newLabel();
        emitter.resolveLabel(copyLoopStart, frameState.computeSnapshot());

        // Jump to end if n == 0
        emitter.loadLocal(nLocal, PrimitiveType.INT);
        emitter.jump(JumpCondition.INT_EQUAL_ZERO, endLabel);

        // Copy the byte: load from data segment, store to memory
        frameState.pushOperand(NumberType.I32);
        frameState.pushOperand(NumberType.I32);

        // Load destination offset
        emitter.loadLocal(dLocal, PrimitiveType.INT);

        // Load byte from data segment at source offset
        emitter.loadLocal(sLocal, PrimitiveType.INT);
        context.getGenerators().getMemoryGenerator().emitLoadData(dataSegmentIndex, segment, context);

        // Store byte to memory
        helper.emitMemoryStoreByte();

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

        // Jump back to loop start
        emitter.jump(JumpCondition.ALWAYS, copyLoopStart);

        // Free the locals
        frameState.freeLocal();
        frameState.freeLocal();
        frameState.freeLocal();

        emitter.resolveLabel(endLabel, frameState.computeSnapshot());
    }
}