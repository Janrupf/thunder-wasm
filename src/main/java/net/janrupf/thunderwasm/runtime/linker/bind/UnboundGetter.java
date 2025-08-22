package net.janrupf.thunderwasm.runtime.linker.bind;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

final class UnboundGetter<T> implements BindableExport<T> {
    private final MethodHandle getter;
    private final boolean isStatic;
    private final Class<T> targetType;

    public static <T> UnboundGetter<T> of(MethodHandles.Lookup lookup, Method m, Class<T> targetType)
            throws ReflectiveRuntimeLinkerException {
        if (m.getParameterCount() != 0) {
            throw new IllegalArgumentException("Getter method must have no parameters: " + m.getName());
        }

        if (!targetType.isAssignableFrom(m.getReturnType())) {
            throw new IllegalArgumentException("Getter method must return a " + targetType.getName() + ": " + m.getName());
        }

        return new UnboundGetter<>(
                ReflectiveRuntimeLinkerException.catching(() -> lookup.unreflect(m)),
                Modifier.isStatic(m.getModifiers()),
                targetType
        );
    }

    public static <T> UnboundGetter<T> of(MethodHandles.Lookup lookup, Field m, Class<T> targetType)
            throws ReflectiveRuntimeLinkerException {
        if (!targetType.isAssignableFrom(m.getType())) {
            throw new IllegalArgumentException("Getter field must be of type " + targetType.getName() + ": " + m.getName());
        }

        return new UnboundGetter<>(
                ReflectiveRuntimeLinkerException.catching(() -> lookup.unreflectGetter(m)),
                Modifier.isStatic(m.getModifiers()),
                targetType
        );
    }

    public UnboundGetter(MethodHandle getter, boolean isStatic, Class<T> targetType) {
        this.getter = getter;
        this.isStatic = isStatic;
        this.targetType = targetType;
    }

    @Override
    public T bind(Object instance) throws ReflectiveRuntimeLinkerException {
        return ReflectiveRuntimeLinkerException.catching(
                () -> targetType.cast(BindableExport.bindHandle(isStatic, instance, getter).invoke())
        );
    }
}
