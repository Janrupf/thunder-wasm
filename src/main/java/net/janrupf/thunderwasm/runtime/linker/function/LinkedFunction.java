package net.janrupf.thunderwasm.runtime.linker.function;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.types.ValueType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface LinkedFunction {
    /**
     * Convert this function to a {@link MethodHandle}.
     * <p>
     * See {@link net.janrupf.thunderwasm.assembler.WasmTypeConverter} for how to convert
     * the types of this function to Java types.
     *
     * @return the method handle representing this function
     */
    MethodHandle asMethodHandle();

    /**
     * Retrieve the argument types of this function.
     *
     * @return the list of argument types
     */
    List<ValueType> getArguments();

    /**
     * Retrieve the return types of this function.
     *
     * @return the list of return types
     */
    List<ValueType> getReturnTypes();

    /**
     * Retrieve the index of the argument that represents the module
     * instance, or -1 if this function does not have a module instance argument.
     * <p>
     * The module instance argument is not reflected in the value of {@link #getArguments()}.
     *
     * @return the index of the module instance argument, or -1 if not applicable
     */
    default int getModuleInstanceArgumentIndex() {
        return -1;
    }

    /**
     * Simple implementation of {@link LinkedFunction}.
     * <p>
     * Used by the default function generator for referencing
     * functions.
     */
    class Simple implements LinkedFunction {
        private final MethodHandle methodHandle;
        private final List<ValueType> arguments;
        private final List<ValueType> returnTypes;
        private final int moduleInstanceArgumentIndex;

        public Simple(MethodHandle methodHandle, List<ValueType> arguments, List<ValueType> returnTypes) {
            this(methodHandle, arguments, returnTypes, -1);
        }

        public Simple(
                MethodHandle methodHandle,
                List<ValueType> arguments,
                List<ValueType> returnTypes,
                int moduleInstanceArgumentIndex
        ) {
            this.methodHandle = methodHandle;
            this.arguments = arguments;
            this.returnTypes = returnTypes;
            this.moduleInstanceArgumentIndex = moduleInstanceArgumentIndex;
        }

        @Override
        public MethodHandle asMethodHandle() {
            return methodHandle;
        }

        @Override
        public List<ValueType> getArguments() {
            return arguments;
        }

        @Override
        public List<ValueType> getReturnTypes() {
            return returnTypes;
        }

        @Override
        public int getModuleInstanceArgumentIndex() {
            return moduleInstanceArgumentIndex;
        }

        /**
         * Infers a {@link Simple} from a {@link MethodHandle}.
         *
         * @param methodHandle the method handle to infer from
         * @return a new {@link Simple} instance
         * @throws WasmAssemblerException if the method handle's types cannot be converted
         */
        public static Simple inferFromMethodHandle(MethodHandle methodHandle) throws WasmAssemblerException {
            return inferFromMethodHandle(methodHandle, -1);
        }

        /**
         * Infers a {@link Simple} from a {@link MethodHandle}.
         *
         * @param methodHandle                the method handle to infer from
         * @param moduleInstanceArgumentIndex the index of the module instance argument, or -1 if not applicable
         * @return a new {@link Simple} instance
         * @throws WasmAssemblerException if the method handle's types cannot be converted
         */
        public static Simple inferFromMethodHandle(
                MethodHandle methodHandle,
                int moduleInstanceArgumentIndex
        ) throws WasmAssemblerException {
            MethodType type = methodHandle.type();

            Class<?>[] argumentClasses = type.parameterArray();
            Class<?> returnClass = type.returnType();

            List<ValueType> argumentTypes = new ArrayList<>(argumentClasses.length);
            for (int i = 0; i < argumentClasses.length; i++) {
                if (i == moduleInstanceArgumentIndex) {
                    continue; // Skip the module instance argument
                }

                argumentTypes.add(WasmTypeConverter.fromJavaType(JavaType.of(argumentClasses[i])));
            }

            List<ValueType> returnTypes = Collections.singletonList(WasmTypeConverter.fromJavaType(JavaType.of(returnClass)));

            return new Simple(methodHandle, argumentTypes, returnTypes, moduleInstanceArgumentIndex);
        }
    }
}
