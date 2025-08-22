package net.janrupf.thunderwasm.runtime.linker.bind;

import net.janrupf.thunderwasm.ThunderWasmException;
import net.janrupf.thunderwasm.data.Limits;
import net.janrupf.thunderwasm.runtime.linker.ProbingRuntimeLinker;
import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;
import net.janrupf.thunderwasm.runtime.linker.global.LinkedGlobal;
import net.janrupf.thunderwasm.runtime.linker.global.LinkedGlobalBase;
import net.janrupf.thunderwasm.runtime.linker.memory.LinkedMemory;
import net.janrupf.thunderwasm.runtime.linker.table.LinkedTable;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;
import net.janrupf.thunderwasm.util.ObjectUtil;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;

/**
 * A {@link ProbingRuntimeLinker} that uses reflection to link functions and other imports.
 * <p>
 * The implementation of this linker is lazy, meaning that it will only link the imports when they are actually needed.
 */
public final class ReflectiveRuntimeLinker implements ProbingRuntimeLinker {
    private final Map<ExportName, LinkedGlobalBase> globals;
    private final Map<ExportName, LinkedTable<?>> tables;
    private final Map<ExportName, LinkedMemory> memories;
    private final Map<ExportName, LinkedFunction> functions;

    private ReflectiveRuntimeLinker(Unbound unbound, Object instance) throws ReflectiveRuntimeLinkerException {
        this.globals = bindAll(unbound.globals, instance);
        this.tables = bindAll(unbound.tables, instance);
        this.memories = bindAll(unbound.memories, instance);
        this.functions = bindAll(unbound.functions, instance);
    }

    private <T> Map<ExportName, T> bindAll(Map<ExportName, BindableExport<T>> bindables, Object instance)
            throws ReflectiveRuntimeLinkerException {
        Map<ExportName, T> bound = new HashMap<>(bindables.size());
        for (Map.Entry<ExportName, BindableExport<T>> e : bindables.entrySet()) {
            bound.put(e.getKey(), e.getValue().bind(instance));
        }
        return bound;
    }

    @Override
    public LinkedGlobalBase tryLinkGlobal(String moduleName, String importName, ValueType type, boolean readOnly)
            throws ThunderWasmException {
        LinkedGlobalBase found = globals.get(new ExportName(moduleName, importName));
        if (found == null) {
            return null;
        }

        if (!found.getType().equals(type)) {
            throw new ThunderWasmException(
                    "Global type mismatch for " + moduleName + "@" + importName + ": expected " + type + ", found " + found.getType()
            );
        }

        if (!(found instanceof LinkedGlobal) && !readOnly) {
            throw new ThunderWasmException(
                    "Global " + moduleName + "@" + importName + " is read-only, but requested as writable"
            );
        }

        return found;
    }

    @Override
    public <T> LinkedTable<T> tryLinkTable(
            String moduleName,
            String importName,
            ReferenceType type,
            Limits limits
    ) {
        return ObjectUtil.forceCast(tables.get(new ExportName(moduleName, importName)));
    }

    @Override
    public LinkedMemory tryLinkMemory(String moduleName, String importName, Limits limits) {
        return memories.get(new ExportName(moduleName, importName));
    }

    @Override
    public LinkedFunction tryLinkFunction(String moduleName, String importName, FunctionType type) throws ThunderWasmException {
        LinkedFunction found = functions.get(new ExportName(moduleName, importName));
        if (found == null) {
            return null;
        }

        if (!found.getArguments().equals(type.getInputs().asFlatList())) {
            throw new ThunderWasmException(
                    "Function argument type mismatch for " + moduleName + "@" + importName + ": expected " +
                            type.getInputs().asFlatList() + ", found " + found.getArguments()
            );
        }

        if (!found.getReturnTypes().equals(type.getOutputs().asFlatList())) {
            throw new ThunderWasmException(
                    "Function return type mismatch for " + moduleName + "@" + importName + ": expected " +
                            type.getOutputs().asFlatList() + ", found " + found.getReturnTypes()
            );
        }

        return found;
    }

    /**
     * Prepare a new {@link Unbound} instance for the given target class.
     * <p>
     * This performs the expensive work of scanning the class for target
     * fields and methods.
     *
     * @param defaultModuleName the default module name to use for exports if not overridden by the annotation
     * @param targetClass       the class to scan for target fields and methods
     * @return a new {@link Unbound} instance
     * @throws ReflectiveRuntimeLinkerException if the class could not be scanned
     */
    public static Unbound of(String defaultModuleName, Class<?> targetClass) throws ReflectiveRuntimeLinkerException {
        return of(defaultModuleName, targetClass, MethodHandles.publicLookup());
    }

    /**
     * Prepare a new {@link Unbound} instance for the given target class with a specific lookup.
     * <p>
     * This performs the expensive work of scanning the class for target
     * fields and methods.
     *
     * @param defaultModuleName the default module name to use for exports if not overridden by the annotation
     * @param targetClass       the class to scan for target fields and methods
     * @param lookup            the {@link MethodHandles.Lookup} to use for accessing methods and fields
     * @return a new {@link Unbound} instance
     * @throws ReflectiveRuntimeLinkerException if the class could not be scanned
     */
    public static Unbound of(String defaultModuleName, Class<?> targetClass, MethodHandles.Lookup lookup)
            throws ReflectiveRuntimeLinkerException {
        return new Unbound(defaultModuleName, targetClass, lookup);
    }

    /**
     * Create a new {@link ReflectiveRuntimeLinker} instance that is bound to the given instance.
     *
     * @param defaultModuleName the default module name to use for exports if not overridden by the annotation
     * @param instance          the instance to bind the exports to
     * @return a new {@link ReflectiveRuntimeLinker} instance with the bound exports
     * @throws ReflectiveRuntimeLinkerException if the binding fails
     */
    public static ReflectiveRuntimeLinker ofBound(String defaultModuleName, Object instance)
            throws ReflectiveRuntimeLinkerException {
        return of(defaultModuleName, instance.getClass()).bind(instance);
    }

    /**
     * Create a new {@link ReflectiveRuntimeLinker} instance that is bound to the given instance.
     *
     * @param defaultModuleName the default module name to use for exports if not overridden by the annotation
     * @param targetClass       the class to scan for target fields and methods
     * @param instance          the instance to bind the exports to
     * @return a new {@link ReflectiveRuntimeLinker} instance with the bound exports
     * @throws ReflectiveRuntimeLinkerException if the binding fails
     */
    public static ReflectiveRuntimeLinker ofBound(
            String defaultModuleName,
            Class<?> targetClass,
            Object instance
    ) throws ReflectiveRuntimeLinkerException {
        return of(defaultModuleName, targetClass).bind(instance);
    }

    public static final class Unbound {
        private final String defaultModuleName;
        private final Class<?> targetClass;
        private final MethodHandles.Lookup lookup;

        private final Map<ExportName, BindableExport<LinkedGlobalBase>> globals;
        private final Map<ExportName, BindableExport<LinkedFunction>> functions;
        private final Map<ExportName, BindableExport<LinkedTable<?>>> tables;
        private final Map<ExportName, BindableExport<LinkedMemory>> memories;

        private Unbound(
                String defaultModuleName,
                Class<?> targetClass,
                MethodHandles.Lookup lookup
        ) throws ReflectiveRuntimeLinkerException {
            this.defaultModuleName = defaultModuleName;
            this.targetClass = targetClass;
            this.lookup = lookup;

            this.globals = new HashMap<>();
            this.functions = new HashMap<>();
            this.tables = new HashMap<>();
            this.memories = new HashMap<>();

            performScan();
        }

        private void performScan() throws ReflectiveRuntimeLinkerException {
            ReflectiveExportCollector collector = new ReflectiveExportCollector(defaultModuleName, targetClass);

            handleMethods(collector.exportMethods());
            handleFields(collector.exportFields());
            handleConstructors(collector.exportConstructors());
        }

        /**
         * Bind the exports to an instance of the target class.
         *
         * @param instance the instance to bind the exports to, or null for binding static exports
         * @return a new {@link ReflectiveRuntimeLinker} instance with the bound exports
         * @throws ReflectiveRuntimeLinkerException if the binding fails
         */
        public ReflectiveRuntimeLinker bind(Object instance) throws ReflectiveRuntimeLinkerException {
            return new ReflectiveRuntimeLinker(this, instance);
        }

        private void handleMethods(List<ExportDescription<Method>> methods) throws ReflectiveRuntimeLinkerException {
            for (ExportDescription<Method> d : methods) {
                handleMethod(d);
            }
        }

        private void handleMethod(ExportDescription<Method> description) throws ReflectiveRuntimeLinkerException {
            WasmExport.Type type = description.getType();
            Method m = description.getExport();

            switch (type) {
                case AUTO:
                case FUNCTION: {
                    addExport(this.functions, description, UnboundFunction.of(lookup, m));
                    break;
                }

                case FUNCTION_GETTER: {
                    addExport(this.functions, description, UnboundGetter.of(lookup, m, LinkedFunction.class));
                    break;
                }

                case TABLE: {
                    addExport(this.tables, description, UnboundGetter.of(lookup, m, ObjectUtil.forceCast(LinkedTable.class)));
                    break;
                }

                case GLOBAL: {
                    addExport(this.globals, description, UnboundGetter.of(lookup, m, LinkedGlobalBase.class));
                    break;
                }

                case MEMORY: {
                    addExport(this.memories, description, UnboundGetter.of(lookup, m, LinkedMemory.class));
                    break;
                }

                default:
                    throw new ReflectiveRuntimeLinkerException(
                            "Unsupported export type for method: " + m.getName() + " in class " + targetClass.getName()
                    );
            }
        }

        private void handleFields(List<ExportDescription<Field>> fields) throws ReflectiveRuntimeLinkerException {
            for (ExportDescription<Field> d : fields) {
                handleField(d);
            }
        }

        private void handleField(ExportDescription<Field> description) throws ReflectiveRuntimeLinkerException {
            WasmExport.Type type = description.getType();
            Field f = description.getExport();

            switch (type) {
                case AUTO:
                case GLOBAL: {
                    addExport(this.globals, description, UnboundGlobal.of(lookup, f, description.isReadOnly()));
                    break;
                }

                case TABLE: {
                    addExport(this.tables, description, UnboundGetter.of(lookup, f, ObjectUtil.forceCast(LinkedTable.class)));
                    break;
                }

                case MEMORY: {
                    addExport(this.memories, description, UnboundGetter.of(lookup, f, LinkedMemory.class));
                    break;
                }

                case FUNCTION: {
                    addExport(this.functions, description, UnboundGetter.of(lookup, f, LinkedFunction.class));
                    break;
                }

                default:
                    throw new ReflectiveRuntimeLinkerException(
                            "Unsupported export type for field: " + f.getName() + " in class " + targetClass.getName()
                    );
            }
        }

        private void handleConstructors(List<ExportDescription<Constructor<?>>> constructors)
                throws ReflectiveRuntimeLinkerException {
            for (ExportDescription<Constructor<?>> d : constructors) {
                handleConstructor(d);
            }
        }

        private void handleConstructor(ExportDescription<Constructor<?>> description)
                throws ReflectiveRuntimeLinkerException {
            WasmExport.Type type = description.getType();
            Constructor<?> c = description.getExport();

            switch (type) {
                case AUTO:
                case FUNCTION: {
                    addExport(this.functions, description, UnboundFunction.of(lookup, c));
                    break;
                }

                default:
                    throw new ReflectiveRuntimeLinkerException(
                            "Unsupported export type for constructor: " + c.getName() + " in class " + targetClass.getName()
                    );
            }
        }

        private <T> void addExport(Map<ExportName, T> exports, ExportDescription<?> description, T export)
                throws ReflectiveRuntimeLinkerException {
            if (exports.containsKey(description.getName())) {
                throw new ReflectiveRuntimeLinkerException(
                        "Duplicate export found: " + description.getName() + " in " + targetClass.getName()
                );
            }

            exports.put(description.getName(), export);
        }
    }
}
