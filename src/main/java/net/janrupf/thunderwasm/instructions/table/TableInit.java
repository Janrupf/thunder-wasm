package net.janrupf.thunderwasm.instructions.table;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.assembler.generator.TableGenerator;
import net.janrupf.thunderwasm.imports.TableImportDescription;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.data.DoubleIndexData;
import net.janrupf.thunderwasm.instructions.data.ElementIndexData;
import net.janrupf.thunderwasm.instructions.data.TableIndexData;
import net.janrupf.thunderwasm.instructions.decoder.InstructionDecoder;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.segment.ElementSegment;
import net.janrupf.thunderwasm.runtime.BoundsChecks;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.TableType;

import java.io.IOException;

public final class TableInit extends WasmInstruction<DoubleIndexData<ElementIndexData, TableIndexData>> {
    public static final TableInit INSTANCE = new TableInit();

    private TableInit() {
        super("table.init", (byte) 0xFC);
    }

    @Override
    public InstructionDecoder getDecoder() {
        return InstructionDecoder.u32Variant(12, this);
    }

    @Override
    public DoubleIndexData<ElementIndexData, TableIndexData> readData(WasmLoader loader)
            throws IOException, InvalidModuleException {
        return new DoubleIndexData<>(
                ElementIndexData.read(loader),
                TableIndexData.read(loader)
        );
    }

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, DoubleIndexData<ElementIndexData, TableIndexData> data)
            throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I32);
        context.getFrameState().popOperand(NumberType.I32);
        context.getFrameState().popOperand(NumberType.I32);

        final ElementIndexData element = data.getFirst();
        final TableIndexData table = data.getSecond();

        final FoundElement<TableType, TableImportDescription> tableElement = context.getLookups().requireTable(table.toArrayIndex());
        final ElementSegment elementSegment = context.getLookups().requireElementSegment(element.toArrayIndex());

        final TableInstructionHelper helper = new TableInstructionHelper(tableElement, context);
        final TableGenerator generator = context.getGenerators().getTableGenerator();

        return new ProcessedInstruction() {
            @Override
            public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
                CodeEmitter codeEmitter = context.getEmitter();

                if (context.getConfiguration().atomicBoundsChecksEnabled()) {
                    CommonBytecodeGenerator.emitPrepareWriteBoundsCheck(codeEmitter);
                    helper.emitTableSize();

                    codeEmitter.invoke(
                            ObjectType.of(BoundsChecks.class),
                            "checkTableBulkWrite",
                            new JavaType[]{PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT},
                            PrimitiveType.VOID,
                            InvokeType.STATIC,
                            false
                    );

                    codeEmitter.duplicate();
                    generator.emitElementSize(element.toArrayIndex(), elementSegment, context);

                    codeEmitter.invoke(
                            ObjectType.of(BoundsChecks.class),
                            "checkElementBulkAccess",
                            new JavaType[]{PrimitiveType.INT, PrimitiveType.INT},
                            PrimitiveType.VOID,
                            InvokeType.STATIC,
                            false
                    );
                }

                if (generator.canEmitInitFor(helper.getJavaTableType())) {
                    CommonBytecodeGenerator.loadBelow(
                            codeEmitter,
                            3,
                            helper.getJavaTableType(),
                            () -> {
                                generator.emitLoadTableReference(table.toArrayIndex(), context);
                            }
                    );
                    generator.emitTableInit(
                            helper.getTableType(),
                            element.toArrayIndex(),
                            elementSegment,
                            context
                    );
                    return;
                }

                emitFallbackInit(helper, elementSegment, element.toArrayIndex(), context);
            }
            
            @Override
            public void processOutputs(CodeEmitContext context) {
            }
        };
    }

    private void emitFallbackInit(
            TableInstructionHelper helper,
            ElementSegment elementSegment,
            LargeArrayIndex elementSegmentIndex,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        if (!elementSegment.getType().equals(helper.getTableType().getElementType())) {
            throw new WasmAssemblerException("Element segment type does not match table element type");
        }

        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        // Stack top:
        // - n
        // - s
        // - d

        // Store n, s and d in locals
        JavaLocal nLocal = emitter.allocateLocal(PrimitiveType.INT);
        JavaLocal sLocal = emitter.allocateLocal(PrimitiveType.INT);
        JavaLocal dLocal = emitter.allocateLocal(PrimitiveType.INT);

        emitter.storeLocal(nLocal);
        emitter.storeLocal(sLocal);
        emitter.storeLocal(dLocal);

        frameState.popOperand(NumberType.I32);
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(NumberType.I32);

        CodeLabel endLabel = emitter.newLabel();

        CodeLabel copyLoopStart = emitter.newLabel();
        emitter.resolveLabel(copyLoopStart);

        // Jump to end if n == 0
        emitter.loadLocal(nLocal);
        emitter.jump(JumpCondition.INT_EQUAL_ZERO, endLabel);

        // Copy the value
        frameState.pushOperand(NumberType.I32);
        frameState.pushOperand(NumberType.I32);

        emitter.loadLocal(dLocal);
        emitter.loadLocal(sLocal);
        context.getGenerators().getTableGenerator().emitLoadElement(elementSegmentIndex, elementSegment, context);
        helper.emitTableSet();

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
        emitter.jump(JumpCondition.ALWAYS, copyLoopStart);

        // Free the locals
        nLocal.free();
        sLocal.free();
        dLocal.free();

        emitter.resolveLabel(endLabel);
    }
}
