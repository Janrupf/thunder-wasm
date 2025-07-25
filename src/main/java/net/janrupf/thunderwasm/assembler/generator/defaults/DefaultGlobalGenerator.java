package net.janrupf.thunderwasm.assembler.generator.defaults;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaFieldHandle;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.assembler.generator.GlobalGenerator;
import net.janrupf.thunderwasm.data.Global;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.GlobalType;
import net.janrupf.thunderwasm.types.ValueType;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;

public class DefaultGlobalGenerator implements GlobalGenerator {
    @Override
    public void addGlobal(LargeArrayIndex index, Global global, ClassFileEmitter emitter) throws WasmAssemblerException {
        boolean isFinal = global.getType().getMutability() == GlobalType.Mutability.CONST;
        JavaType type = WasmTypeConverter.toJavaType(global.getType().getValueType());

        emitter.field(
                getGlobalFieldName(index),
                Visibility.PRIVATE,
                false,
                isFinal,
                type,
                null
        );
    }

    @Override
    public void emitGetGlobal(LargeArrayIndex index, Global global, CodeEmitContext context) throws WasmAssemblerException {
        CommonBytecodeGenerator.loadThisBelow(context.getEmitter(),  context.getLocalVariables(), 0);
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

        CommonBytecodeGenerator.loadThisBelow(emitter, context.getLocalVariables(), 1);

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

    @Override
    public void makeGlobalExportable(LargeArrayIndex index, Global global, ClassEmitContext context) {
        // No-op, globals are exportable by default
    }

    @Override
    public void emitLoadGlobalExport(LargeArrayIndex index, Global global, CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();
        ValueType globalType = global.getType().getValueType();

        DefaultFieldTypeLookup.Selected selected = DefaultFieldTypeLookup.GLOBAL_HANDLE.select(
                globalType,
                global.getType().getMutability() == GlobalType.Mutability.CONST
        );

        ObjectType selectedHandleType = selected.getType();

        emitter.doNew(selectedHandleType);
        emitter.duplicate();

        List<JavaType> parameterTypes = new ArrayList<>();
        if (selected.isGeneric()) {
            parameterTypes.add(ObjectType.of(ValueType.class));
            CommonBytecodeGenerator.loadTypeReference(emitter, globalType);
        }

        parameterTypes.add(ObjectType.of(MethodHandle.class));
        emitter.loadConstant(new JavaFieldHandle(
                emitter.getOwner(),
                getGlobalFieldName(index),
                WasmTypeConverter.toJavaType(globalType),
                false,
                false,
                false
        ));
        emitter.loadLocal(context.getLocalVariables().getThis());
        CommonBytecodeGenerator.bindMethodHandle(emitter);


        if (global.getType().getMutability() == GlobalType.Mutability.VAR) {
            parameterTypes.add(ObjectType.of(MethodHandle.class));

            emitter.loadConstant(new JavaFieldHandle(
                    emitter.getOwner(),
                    getGlobalFieldName(index),
                    WasmTypeConverter.toJavaType(globalType),
                    false,
                    true,
                    false
            ));
            emitter.loadLocal(context.getLocalVariables().getThis());
            CommonBytecodeGenerator.bindMethodHandle(emitter);
        }
        emitter.invoke(
                selectedHandleType,
                "<init>",
                parameterTypes.toArray(new JavaType[0]),
                PrimitiveType.VOID,
                InvokeType.SPECIAL,
                false
        );
    }

    protected String getGlobalFieldName(LargeArrayIndex index) {
        return "global" + index;
    }
}
