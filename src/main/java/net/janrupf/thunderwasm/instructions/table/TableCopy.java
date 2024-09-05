package net.janrupf.thunderwasm.instructions.table;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.assembler.generator.TableGenerator;
import net.janrupf.thunderwasm.imports.TableImportDescription;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.data.DoubleIndexData;
import net.janrupf.thunderwasm.instructions.data.TableIndexData;
import net.janrupf.thunderwasm.instructions.decoder.InstructionDecoder;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.TableType;

import java.io.IOException;

public final class TableCopy extends WasmInstruction<DoubleIndexData<TableIndexData, TableIndexData>> {
    public static final TableCopy INSTANCE = new TableCopy();

    private TableCopy() {
        super("table.copy", (byte) 0xFC);
    }

    @Override
    public InstructionDecoder getDecoder() {
        return InstructionDecoder.u32Variant(14, this);
    }

    @Override
    public DoubleIndexData<TableIndexData, TableIndexData> readData(WasmLoader loader)
            throws IOException, InvalidModuleException {
        return new DoubleIndexData<>(
                TableIndexData.read(loader),
                TableIndexData.read(loader)
        );
    }

    @Override
    public void emitCode(CodeEmitContext context, DoubleIndexData<TableIndexData, TableIndexData> data)
            throws WasmAssemblerException {
        TableIndexData target = data.getFirst();
        TableIndexData source = data.getSecond();

        LargeArrayIndex sourceIndex = source.toArrayIndex();
        FoundElement<TableType, TableImportDescription> sourceElement = context.getLookups().requireTable(sourceIndex);

        LargeArrayIndex targetIndex = target.toArrayIndex();
        FoundElement<TableType, TableImportDescription> targetElement = context.getLookups().requireTable(targetIndex);

        TableGenerator internalTableGenerator = context.getGenerators().getTableGenerator();

        // Extract the necessary information
        TableInstructionHelper sourceHelper = new TableInstructionHelper(sourceElement, context);
        TableInstructionHelper targetHelper = new TableInstructionHelper(targetElement, context);

        // If the internal generate can emit a copy for the given types, use it
        if (internalTableGenerator.canEmitCopyFor(sourceHelper.getJavaTableType(), targetHelper.getJavaTableType())) {
            WasmFrameState frameState = context.getFrameState();
            CodeEmitter emitter = context.getEmitter();

            CommonBytecodeGenerator.loadBelow(
                    frameState,
                    emitter,
                    3,
                    ReferenceType.OBJECT,
                    () -> {
                        targetHelper.emitLoadTableReference();

                        // The appropriate push will be done by the generator
                        frameState.popOperand(ReferenceType.OBJECT);
                    }
            );

            sourceHelper.emitLoadTableReference();

            internalTableGenerator.emitTableCopy(sourceHelper.getTableType(), targetHelper.getTableType(), context);
            return;
        }

        // If the internal generator cannot emit a copy, emit a fallback copy
        emitFallbackCopy(sourceHelper, targetHelper, context);
    }

    private void emitFallbackCopy(
            TableInstructionHelper sourceHelper,
            TableInstructionHelper targetHelper,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        if (!sourceHelper.getTableType().getElementType().equals(targetHelper.getTableType().getElementType())) {
            throw new WasmAssemblerException(
                    "Table copy not supported for types " + sourceHelper.getTableType().getElementType() +
                            " and " + targetHelper.getTableType().getElementType());
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
        sourceHelper.emitTableGet();
        targetHelper.emitTableSet();

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
        sourceHelper.emitTableGet();
        targetHelper.emitTableSet();

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
        frameState.freeLocal();

        emitter.resolveLabel(endLabel, frameState.computeSnapshot());
    }
}
