package net.janrupf.thunderwasm.assembler.generator;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.data.Global;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.GlobalType;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;

public class DefaultGlobalGenerator implements GlobalGenerator {
    @Override
    public void addGlobal(LargeArrayIndex index, Global global, ClassFileEmitter emitter) throws WasmAssemblerException {
        boolean isFinal = global.getType().getMutability() == GlobalType.Mutability.CONST;
        JavaType type = WasmTypeConverter.toJavaType(global.getType().getValueType());

        emitter.field(
                getGlobalFieldName(index),
                Visibility.PRIVATE, // TODO: exports
                false,
                isFinal,
                type
        );
    }

    @Override
    public void emitGetGlobal(LargeArrayIndex index, Global global, CodeEmitContext context) throws WasmAssemblerException {
        context.getEmitter().loadThis();
        context.getEmitter().accessField(
                context.getEmitter().getOwner(),
                getGlobalFieldName(index),
                WasmTypeConverter.toJavaType(global.getType().getValueType()),
                false,
                false
        );

        // Push the value of the global onto the stack
        WasmFrameState frameState = context.getFrameState();
        frameState.pushOperand(global.getType().getValueType());
    }

    @Override
    public void emitSetGlobal(LargeArrayIndex index, Global global, CodeEmitContext context) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        ValueType globalType = global.getType().getValueType();
        JavaType javaType = WasmTypeConverter.toJavaType(globalType);

        frameState.pushOperand(ReferenceType.EXTERNREF);

        if (javaType.getSlotCount() < 2) {
            // Load this and swap
            emitter.loadThis();
            emitter.op(Op.SWAP);
        } else {
            // Swap using a local
            int localIndex = frameState.computeJavaLocalIndex(frameState.allocateLocal(globalType));
            emitter.storeLocal(localIndex, javaType);

            emitter.loadThis();

            emitter.loadLocal(localIndex, javaType);
            frameState.freeLocal();
        }

        // And store the value
        context.getEmitter().accessField(
                context.getEmitter().getOwner(),
                getGlobalFieldName(index),
                WasmTypeConverter.toJavaType(global.getType().getValueType()),
                false,
                true
        );

        // Pop this and the global
        frameState.popOperand(ReferenceType.EXTERNREF);
        frameState.popOperand(globalType);
    }

    protected String getGlobalFieldName(LargeArrayIndex index) {
        return "global" + index;
    }
}
