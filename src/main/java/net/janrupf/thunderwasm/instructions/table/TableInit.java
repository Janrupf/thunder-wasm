package net.janrupf.thunderwasm.instructions.table;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.assembler.generator.TableGenerator;
import net.janrupf.thunderwasm.imports.TableImportDescription;
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
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ReferenceType;
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
    public void emitCode(CodeEmitContext context, DoubleIndexData<ElementIndexData, TableIndexData> data)
            throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter codeEmitter = context.getEmitter();

        ElementIndexData element = data.getFirst();
        TableIndexData table = data.getSecond();

        FoundElement<TableType, TableImportDescription> tableElement = context.getLookups().requireTable(table.toArrayIndex());
        ElementSegment elementSegment = context.getLookups().requireElementSegment(element.toArrayIndex());

        TableInstructionHelper helper = new TableInstructionHelper(tableElement, context);

        TableGenerator generator = context.getGenerators().getTableGenerator();

        if (generator.canEmitInitFor(helper.getJavaTableType())) {
            CommonBytecodeGenerator.loadBelow(
                    frameState,
                    codeEmitter,
                    3,
                    ReferenceType.OBJECT,
                    () -> {
                        generator.emitLoadTableReference(table.toArrayIndex(), context);
                        // Will be pushed correctly by the generator
                        frameState.popOperand(ReferenceType.OBJECT);
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

        // Copy the value
        frameState.pushOperand(NumberType.I32);
        frameState.pushOperand(NumberType.I32);

        emitter.loadLocal(dLocal, PrimitiveType.INT);
        emitter.loadLocal(sLocal, PrimitiveType.INT);
        context.getGenerators().getTableGenerator().emitLoadElement(elementSegmentIndex, elementSegment, context);
        helper.emitTableSet();

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
        emitter.jump(JumpCondition.ALWAYS, copyLoopStart);

        // Free the locals
        frameState.freeLocal();
        frameState.freeLocal();
        frameState.freeLocal();

        emitter.resolveLabel(endLabel, frameState.computeSnapshot());
    }
}
