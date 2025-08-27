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
import net.janrupf.thunderwasm.instructions.data.DataIndexData;
import net.janrupf.thunderwasm.instructions.data.DoubleIndexData;
import net.janrupf.thunderwasm.instructions.data.MemoryIndexData;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.segment.DataSegment;
import net.janrupf.thunderwasm.runtime.BoundsChecks;
import net.janrupf.thunderwasm.types.MemoryType;
import net.janrupf.thunderwasm.types.NumberType;

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
    public ProcessedInstruction processInputs(CodeEmitContext context, DoubleIndexData<DataIndexData, MemoryIndexData> data)
            throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I32); // n (count)
        context.getFrameState().popOperand(NumberType.I32); // s (source start index in data segment)
        context.getFrameState().popOperand(NumberType.I32); // d (destination start index in memory)

        final DataIndexData dataSegment = data.getFirst();
        final MemoryIndexData memory = data.getSecond();

        final FoundElement<MemoryType, MemoryImportDescription> memoryElement = context.getLookups().requireMemory(memory.toArrayIndex());
        final DataSegment segment = context.getLookups().requireDataSegment(dataSegment.toArrayIndex());

        return new ProcessedInstruction() {
            @Override
            public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
                CodeEmitter codeEmitter = context.getEmitter();
                MemoryInstructionHelper helper = new MemoryInstructionHelper(memoryElement, context);
                MemoryGenerator internalGenerator = context.getGenerators().getMemoryGenerator();

                if (context.getConfiguration().atomicBoundsChecksEnabled()) {
                    CommonBytecodeGenerator.emitPrepareWriteBoundsCheck(codeEmitter);
                    helper.emitMemorySize();

                    codeEmitter.invoke(
                            ObjectType.of(BoundsChecks.class),
                            "checkMemoryBulkWrite",
                            new JavaType[]{PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT},
                            PrimitiveType.VOID,
                            InvokeType.STATIC,
                            false
                    );

                    codeEmitter.duplicate();
                    internalGenerator.emitDataSegmentSize(dataSegment.toArrayIndex(), segment, context);

                    codeEmitter.invoke(
                            ObjectType.of(BoundsChecks.class),
                            "checkDataBulkAccess",
                            new JavaType[]{PrimitiveType.INT, PrimitiveType.INT},
                            PrimitiveType.VOID,
                            InvokeType.STATIC,
                            false
                    );
                }

                if (internalGenerator.canEmitInitFor(helper.getJavaMemoryType())) {
                    CommonBytecodeGenerator.loadBelow(
                            codeEmitter,
                            3,
                            helper.getJavaMemoryType(),
                            helper::emitLoadMemoryReference
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
            
            @Override
            public void processOutputs(CodeEmitContext context) {
            }
        };
    }

    private void emitFallbackInit(
            MemoryInstructionHelper helper,
            DataSegment segment,
            LargeArrayIndex dataSegmentIndex,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        // Stack top:
        // - n (count)
        // - s (source start index in data segment)
        // - d (destination start index in memory)

        // Store n, s and d in locals
        JavaLocal nLocal = emitter.allocateLocal(PrimitiveType.INT);
        JavaLocal sLocal = emitter.allocateLocal(PrimitiveType.INT);
        JavaLocal dLocal = emitter.allocateLocal(PrimitiveType.INT);

        emitter.storeLocal(nLocal);
        emitter.storeLocal(sLocal);
        emitter.storeLocal(dLocal);

        CodeLabel endLabel = emitter.newLabel();

        CodeLabel copyLoopStart = emitter.newLabel();
        emitter.resolveLabel(copyLoopStart);

        // Jump to end if n == 0
        emitter.loadLocal(nLocal);
        emitter.jump(JumpCondition.INT_EQUAL_ZERO, endLabel);

        // Copy the byte: load from data segment, store to memory
        // Load destination offset
        emitter.loadLocal(dLocal);

        // Load byte from data segment at source offset
        emitter.loadLocal(sLocal);
        context.getGenerators().getMemoryGenerator().emitLoadData(dataSegmentIndex, segment, context);

        // Store byte to memory
        helper.emitMemoryStoreByte();

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

        // Jump back to loop start
        emitter.jump(JumpCondition.ALWAYS, copyLoopStart);

        // Free the locals
        nLocal.free();
        sLocal.free();
        dLocal.free();

        emitter.resolveLabel(endLabel);
    }
}