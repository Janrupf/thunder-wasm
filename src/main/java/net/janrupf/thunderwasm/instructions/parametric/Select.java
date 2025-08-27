package net.janrupf.thunderwasm.instructions.parametric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.UnknownType;
import net.janrupf.thunderwasm.types.ValueType;
import net.janrupf.thunderwasm.types.VecType;

import java.io.IOException;

public final class Select extends WasmInstruction<Select.SelectData> {
    public static final Select VARIANT_WITHOUT_TYPES = new Select((byte) 0x1B, false);
    public static final Select VARIANT_WITH_TYPES = new Select((byte) 0x1C, true);

    private final boolean acceptsTypes;

    private Select(byte opCode, boolean acceptsTypes) {
        super("select", opCode);
        this.acceptsTypes = acceptsTypes;
    }

    @Override
    public SelectData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        if (!this.acceptsTypes) {
            return new SelectData(null);
        }

        LargeArray<ValueType> types = loader.readVec(
                ValueType.class,
                loader::readValueType
        );

        return new SelectData(types);
    }

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, SelectData data) throws WasmAssemblerException {
        if (data.getTypes() != null && data.getTypes().length() != 1) {
            throw new WasmAssemblerException("Expected exactly 1 type, got " + data.getTypes().length());
        }

        WasmFrameState frameState = context.getFrameState();
        LargeArray<ValueType> types = data.getTypes();

        frameState.popOperand(NumberType.I32);

        if (types == null) {
            ValueType targetType = frameState.popAnyOperand();
            
            if (!(targetType instanceof NumberType) && !(targetType instanceof VecType) && !(targetType instanceof UnknownType)) {
                throw new WasmAssemblerException("Expected number or vector type, got " + targetType.getName());
            }

            frameState.popOperand(targetType);
            types = LargeArray.of(ValueType.class, targetType);
        } else {
            ValueType firstType = types.get(LargeArrayIndex.ZERO);
            frameState.popOperand(firstType);
            frameState.popOperand(firstType);
        }

        final LargeArray<ValueType> finalTypes = types;
        final ValueType resultType = finalTypes.get(LargeArrayIndex.ZERO);
        final boolean isSingleSlot = resultType instanceof UnknownType /* will never emit code either way */||
                finalTypes.length() < 2 && (WasmTypeConverter.toJavaType(resultType).getSlotCount() < 2);

        return new ProcessedInstruction() {
            @Override
            public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
                if (isSingleSlot) {
                    // Use efficient single-slot select implementation
                    emitCodeSingleSlotSelect(context, resultType);
                } else {
                    // Use general multi-slot select implementation
                    emitMultiSlotSelect(context, finalTypes);
                }
            }

            @Override
            public void processOutputs(CodeEmitContext context) throws WasmAssemblerException {
                context.getFrameState().pushOperand(resultType);
            }
        };
    }

    private void emitCodeSingleSlotSelect(
            CodeEmitContext context,
            ValueType targetType
    ) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        CodeLabel endLabel = emitter.newLabel();

        // Jump to dropping the false value if the condition is true
        emitter.jump(JumpCondition.INT_NOT_EQUAL_ZERO, endLabel);

        // Condition is false, swap the values
        emitter.op(Op.SWAP);

        // Drop the value on top
        emitter.resolveLabel(endLabel);
        emitter.pop();
    }

    private void emitMultiSlotSelect(
            CodeEmitContext context,
            LargeArray<ValueType> types
    ) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        CodeLabel elseLabel = emitter.newLabel();
        CodeLabel endLabel = emitter.newLabel();

        // Jump to else if the condition is false
        emitter.jump(JumpCondition.INT_EQUAL_ZERO, elseLabel);

        // Fall through - condition is true
        // Drop all the false values on top
        LargeArrayIndex i = types.largeLength().subtract(1);
        while (true) {
            emitter.pop();

            if (i.equals(LargeArrayIndex.ZERO)) {
                break;
            }

            i = i.subtract(1);
        }

        // Jump to the end
        emitter.jump(JumpCondition.ALWAYS, endLabel);

        // Condition is false, this makes it more annoying
        // we need to drop the true values and keep the false values
        emitter.resolveLabel(elseLabel);

        // Allocate locals for the false values
        LargeArray<JavaLocal> localIndices = new LargeArray<>(JavaLocal.class, types.largeLength());

        i = types.largeLength().subtract(1);
        while (true) {
            ValueType type = types.get(i);
            JavaLocal localIndex = emitter.allocateLocal(WasmTypeConverter.toJavaType(type));
            localIndices.set(i, localIndex);

            // And store the value in the local
            emitter.storeLocal(localIndex);

            if (i.equals(LargeArrayIndex.ZERO)) {
                break;
            }

            i = i.subtract(1);
        }

        // Drop all the true values on top
        i = types.largeLength().subtract(1);
        while (true) {
            emitter.pop();

            if (i.equals(LargeArrayIndex.ZERO)) {
                break;
            }

            i = i.subtract(1);
        }

        // Load the false values back
        for (i = LargeArrayIndex.ZERO; i.compareTo(types.largeLength()) < 0; i = i.add(1)) {
            JavaLocal local = localIndices.get(i);
            emitter.loadLocal(local);
            local.free();
        }

        // Done
        emitter.resolveLabel(endLabel);
    }

    public static class SelectData implements WasmInstruction.Data {
        private final LargeArray<ValueType> types;

        public SelectData(LargeArray<ValueType> types) {
            this.types = types;
        }

        public LargeArray<ValueType> getTypes() {
            return types;
        }
    }
}
