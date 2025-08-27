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
import net.janrupf.thunderwasm.instructions.data.TableIndexData;
import net.janrupf.thunderwasm.instructions.decoder.InstructionDecoder;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.runtime.BoundsChecks;
import net.janrupf.thunderwasm.types.NumberType;
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
    public ProcessedInstruction processInputs(CodeEmitContext context, DoubleIndexData<TableIndexData, TableIndexData> data)
            throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I32); // count
        context.getFrameState().popOperand(NumberType.I32); // source
        context.getFrameState().popOperand(NumberType.I32); // destination

        final TableIndexData target = data.getFirst();
        final TableIndexData source = data.getSecond();

        final FoundElement<TableType, TableImportDescription> sourceElement = context.getLookups().requireTable(source.toArrayIndex());
        final FoundElement<TableType, TableImportDescription> targetElement = context.getLookups().requireTable(target.toArrayIndex());

        final TableInstructionHelper sourceHelper = new TableInstructionHelper(sourceElement, context);
        final TableInstructionHelper targetHelper = new TableInstructionHelper(targetElement, context);
        final TableGenerator internalTableGenerator = context.getGenerators().getTableGenerator();

        return new ProcessedInstruction() {
            @Override
            public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
                CodeEmitter emitter = context.getEmitter();

                if (context.getConfiguration().atomicBoundsChecksEnabled()) {
                    CommonBytecodeGenerator.emitPrepareCopyBoundsCheck(emitter);
                    sourceHelper.emitTableSize();
                    targetHelper.emitTableSize();

                    emitter.invoke(
                            ObjectType.of(BoundsChecks.class),
                            "checkTableCopyBulkAccess",
                            new JavaType[]{PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT},
                            PrimitiveType.VOID,
                            InvokeType.STATIC,
                            false
                    );
                }

                // If the internal generate can emit a copy for the given types, use it
                if (internalTableGenerator.canEmitCopyFor(sourceHelper.getJavaTableType(), targetHelper.getJavaTableType())) {

                    CommonBytecodeGenerator.loadBelow(
                            emitter,
                            3,
                            targetHelper.getJavaTableType(),
                            targetHelper::emitLoadTableReference
                    );

                    sourceHelper.emitLoadTableReference();

                    internalTableGenerator.emitTableCopy(sourceHelper.getTableType(), targetHelper.getTableType(), context);
                    return;
                }

                // If the internal generator cannot emit a copy, emit a fallback copy
                emitFallbackCopy(sourceHelper, targetHelper, context);
            }
            
            @Override
            public void processOutputs(CodeEmitContext context) throws WasmAssemblerException {
                // Phase 3: Process outputs - TableCopy produces no operands
            }
        };
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

        // Copy the value
        frameState.pushOperand(NumberType.I32);
        frameState.pushOperand(NumberType.I32);

        emitter.loadLocal(dLocal);
        emitter.loadLocal(sLocal);
        sourceHelper.emitTableGet();
        targetHelper.emitTableSet();

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
        frameState.pushOperand(NumberType.I32);
        emitter.loadLocal(dLocal);
        emitter.loadLocal(nLocal);
        emitter.loadConstant(1);
        emitter.op(Op.ISUB);
        emitter.op(Op.IADD);

        // Push s + n - 1
        frameState.pushOperand(NumberType.I32);
        emitter.loadLocal(sLocal);
        emitter.loadLocal(nLocal);
        emitter.loadConstant(1);
        emitter.op(Op.ISUB);
        emitter.op(Op.IADD);

        // Copy the value
        sourceHelper.emitTableGet();
        targetHelper.emitTableSet();

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
