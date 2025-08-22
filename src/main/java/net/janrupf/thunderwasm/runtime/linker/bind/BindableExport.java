package net.janrupf.thunderwasm.runtime.linker.bind;

import java.lang.invoke.MethodHandle;

/**
 * Represents a currently unbound export.
 *
 * @param <T> the type that binding will produce
 */
public interface BindableExport<T> {
    /**
     * Bind the export to a Java object instance.
     *
     * @param instance the instance to bind to
     * @return the bound export
     * @throws ReflectiveRuntimeLinkerException if the binding fails
     */
    T bind(Object instance) throws ReflectiveRuntimeLinkerException;

    /**
     * Helper for binding a method handle to an instance if required.
     *
     * @param isStatic whether the method handle is static
     * @param instance the instance to bind to
     * @return the bound method handle
     * @throws ReflectiveRuntimeLinkerException if the binding fails
     */
    static MethodHandle bindHandle(boolean isStatic, Object instance, MethodHandle handle)
            throws ReflectiveRuntimeLinkerException {
        if (isStatic || handle == null) {
            // No binding needed for static methods or missing handles
            return handle;
        }

        if (instance == null) {
            throw new ReflectiveRuntimeLinkerException("Cannot bind non-static method handle to null instance");
        }

        try {
            return handle.bindTo(instance);
        } catch (Exception e) {
            throw new ReflectiveRuntimeLinkerException("Failed to bind method handle to instance", e);
        }
    }
}
