package net.janrupf.thunderwasm.runtime.linker.bind;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Helper to collect exported fields from a target class using reflection.
 */
public class ReflectiveExportCollector {
    private final String defaultModuleName;
    private final Class<?> targetClass;

    /**
     * Create a new ReflectiveExportCollector.
     *
     * @param defaultModuleName the default module name to use for exports if not overridden by the annotation
     * @param targetClass       the class to scan for exported fields
     */
    public ReflectiveExportCollector(String defaultModuleName, Class<?> targetClass) {
        this.defaultModuleName = defaultModuleName;
        this.targetClass = targetClass;
    }

    /**
     * Find and build a list of exported fields from the target class, making them accessible if necessary.
     *
     * @return a list of {@link ExportDescription} for each exported field
     * @throws ReflectiveRuntimeLinkerException if the target class could not be accessed or scanned
     */
    public List<ExportDescription<Field>> exportFields() throws ReflectiveRuntimeLinkerException {
        return resolveExports(tryAccess(Class::getDeclaredFields, Class::getFields));
    }

    /**
     * Find and build a list of exported fields from the target class, making them accessible if necessary.
     *
     * @return a list of {@link ExportDescription} for each exported method
     * @throws ReflectiveRuntimeLinkerException if the target class could not be accessed or scanned
     */
    public List<ExportDescription<Method>> exportMethods() throws ReflectiveRuntimeLinkerException {
        return resolveExports(tryAccess(Class::getDeclaredMethods, Class::getMethods));
    }

    /**
     * Find and build a list of exported fields from the target class, making them accessible if necessary.
     *
     * @return a list of {@link ExportDescription} for each exported constructor
     * @throws ReflectiveRuntimeLinkerException if the target class could not be accessed or scanned
     */
    public List<ExportDescription<Constructor<?>>> exportConstructors() throws ReflectiveRuntimeLinkerException {
        return resolveExports(tryAccess(Class::getDeclaredConstructors, Class::getConstructors));
    }

    private <T> T tryAccess(
            Function<Class<?>, T> declaredAccessor,
            Function<Class<?>, T> publicAccessor
    ) throws ReflectiveRuntimeLinkerException {
        try {
            return declaredAccessor.apply(targetClass);
        } catch (SecurityException ignored) {
        } catch (Exception e) {
            throw new ReflectiveRuntimeLinkerException("Failed to access target class: " + targetClass.getName());
        }

        try {
            return publicAccessor.apply(targetClass);
        } catch (Exception e) {
            throw new ReflectiveRuntimeLinkerException("Failed to access target class: " + targetClass.getName());
        }
    }

    private <T extends AccessibleObject & AnnotatedElement & Member> List<ExportDescription<T>> resolveExports(
            T[] members
    ) throws ReflectiveRuntimeLinkerException {
        List<ExportDescription<T>> exports = new ArrayList<>();

        for (T member : members) {
            WasmExport annotation = member.getAnnotation(WasmExport.class);

            if (annotation != null) {
                String moduleName = defaultEmpty(annotation.module(), defaultModuleName);
                String exportName = defaultEmpty(annotation.value(), member.getName());
                boolean readOnly = annotation.readOnly();

                try {
                    member.setAccessible(true);
                } catch (Exception e) {
                    throw new ReflectiveRuntimeLinkerException(
                            "Failed to make member " + member.getClass().getSimpleName() + " accessible: " +
                                    member.getName() + " in class " + targetClass.getName(),
                            e
                    );
                }

                exports.add(new ExportDescription<>(member, moduleName, exportName, readOnly, annotation.type()));
            }
        }

        return exports;
    }

    private String defaultEmpty(String s, String d) {
        if (s == null || s.isEmpty()) {
            return d;
        }

        return s;
    }
}
