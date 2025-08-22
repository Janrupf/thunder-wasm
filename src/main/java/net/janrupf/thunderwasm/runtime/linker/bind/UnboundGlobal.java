package net.janrupf.thunderwasm.runtime.linker.bind;

import net.janrupf.thunderwasm.runtime.linker.global.*;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ValueType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

final class UnboundGlobal implements BindableExport<LinkedGlobalBase> {
    private final MethodHandle getter;
    private final MethodHandle setter;
    private final boolean isStatic;
    private final ValueType valueType;

    public static UnboundGlobal of(MethodHandles.Lookup lookup, Field m, boolean forceReadOnly)
            throws ReflectiveRuntimeLinkerException {
        ValueType valueType = WasmRuntimeTypeInference.inferWasmType(m.getType());

        MethodHandle setter = null;
        if (!Modifier.isFinal(m.getModifiers()) && !forceReadOnly) {
            setter = ReflectiveRuntimeLinkerException.catching(() -> lookup.unreflectSetter(m));
        }

        return new UnboundGlobal(
                ReflectiveRuntimeLinkerException.catching(() -> lookup.unreflectGetter(m)),
                setter,
                Modifier.isStatic(m.getModifiers()),
                valueType
        );
    }

    public UnboundGlobal(MethodHandle getter, MethodHandle setter, boolean isStatic, ValueType valueType) {
        this.getter = getter;
        this.setter = setter;
        this.isStatic = isStatic;
        this.valueType = valueType;
    }

    @Override
    public LinkedGlobalBase bind(Object instance) throws ReflectiveRuntimeLinkerException {
        MethodHandle boundGetter = BindableExport.bindHandle(isStatic, instance, getter);
        MethodHandle boundSetter = BindableExport.bindHandle(isStatic, instance, setter);

        if (valueType == NumberType.I32) {
            if (setter == null) {
                return new LinkedReadOnlyIntGlobal.Handle(boundGetter);
            }

            return new LinkedIntGlobal.Handle(boundGetter, boundSetter);
        } else if (valueType == NumberType.I64) {
            if (setter == null) {
                return new LinkedReadOnlyLongGlobal.Handle(boundGetter);
            }

            return new LinkedLongGlobal.Handle(boundGetter, boundSetter);
        } else if (valueType == NumberType.F32) {
            if (setter == null) {
                return new LinkedReadOnlyFloatGlobal.Handle(boundGetter);
            }

            return new LinkedFloatGlobal.Handle(boundGetter, boundSetter);
        } else if (valueType == NumberType.F64) {
            if (setter == null) {
                return new LinkedReadOnlyDoubleGlobal.Handle(boundGetter);
            }

            return new LinkedDoubleGlobal.Handle(boundGetter, boundSetter);
        } else {
            if (setter == null) {
                return new LinkedReadOnlyObjectGlobal.Handle<>(valueType, boundGetter);
            }

            return new LinkedObjectGlobal.Handle<>(valueType, boundGetter, boundSetter);
        }
    }
}
