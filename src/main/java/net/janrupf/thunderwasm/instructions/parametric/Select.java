package net.janrupf.thunderwasm.instructions.parametric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.NumberType;
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

        if (types.length() != 1) {
            throw new InvalidModuleException("Expected exactly 1 type, got " + types.length());
        }

        return new SelectData(types);
    }

    @Override
    public void emitCode(CodeEmitContext context, SelectData data) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        LargeArray<ValueType> types = data.getTypes();

        frameState.popOperand(NumberType.I32);

        if (types == null) {
            // Pop the next 2 operands
            ValueType targetType = frameState.popAnyOperand();
            if (!(targetType instanceof NumberType) && !(targetType instanceof VecType)) {
                throw new WasmAssemblerException("Expected number or vector type, got " + targetType.getName());
            }
            frameState.popOperand(targetType);

            // And push them back for further processing...
            frameState.pushOperand(targetType);
            frameState.pushOperand(targetType);

            types = LargeArray.of(ValueType.class, targetType);
        }

        ValueType firstType = types.get(LargeArrayIndex.ZERO);

        if (types.length() < 2 && WasmTypeConverter.toJavaType(firstType).getSlotCount() < 2) {
            // We only need to select a single slot, use a more efficient select implementation
            // which doesn't require the use of a local variable
            emitCodeSingleSlotSelect(context, firstType);
        } else {
            // We need to select multiple slots, use the general select implementation
            emitMultiSlotSelect(context, types);
        }
    }

    private void emitCodeSingleSlotSelect(
            CodeEmitContext context,
            ValueType targetType
    ) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();
        WasmFrameState frameState = context.getFrameState();

        CodeLabel endLabel = emitter.newLabel();

        // Jump to dropping the false value if the condition is true
        emitter.jump(JumpCondition.INT_NOT_EQUAL_ZERO, endLabel);

        // Condition is false, swap the values
        emitter.op(Op.SWAP);

        // Drop the value on top
        emitter.resolveLabel(endLabel);
        emitter.pop();

        frameState.popOperand(targetType);
    }

    private void emitMultiSlotSelect(
            CodeEmitContext context,
            LargeArray<ValueType> types
    ) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        // We expect the types on top to be the same as specified in the instruction, but twice
        LargeArray<ValueType> typesTimesTwo = new LargeArray<>(
                ValueType.class,
                types.largeLength().add(types.largeLength())
        );

        for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(types.largeLength()) < 0; i = i.add(1)) {
            ValueType type = types.get(i);
            typesTimesTwo.set(i, type);
            typesTimesTwo.set(i.add(types.largeLength()), type);
        }

        frameState.requireOperands(typesTimesTwo);

        // Validation complete

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
            ValueType t = types.get(i);
            emitter.pop();

            // We also use this loop as an opportunity to notify the frame state
            // of its final form
            frameState.popOperand(t);

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
