package net.janrupf.thunderwasm.assembler.generator.defaults;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.generator.GlobalGenerator;
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
                type,
                null
        );
    }

    @Override
    public void emitGetGlobal(LargeArrayIndex index, Global global, CodeEmitContext context) throws WasmAssemblerException {
        CommonBytecodeGenerator.loadThisBelow(context.getEmitter(), 0);
        context.getEmitter().accessField(
                context.getEmitter().getOwner(),
                getGlobalFieldName(index),
                WasmTypeConverter.toJavaType(global.getType().getValueType()),
                false,
                false
        );
    }

    @Override
    public void emitSetGlobal(LargeArrayIndex index, Global global, CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        ValueType globalType = global.getType().getValueType();

        CommonBytecodeGenerator.loadThisBelow(emitter, 1);

        // And store the value
        context.getEmitter().accessField(
                context.getEmitter().getOwner(),
                getGlobalFieldName(index),
                WasmTypeConverter.toJavaType(globalType),
                false,
                true
        );

        // Pop this and the global
    }

    protected String getGlobalFieldName(LargeArrayIndex index) {
        return "global" + index;
    }
}
