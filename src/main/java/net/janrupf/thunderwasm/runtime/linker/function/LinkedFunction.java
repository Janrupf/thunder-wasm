package net.janrupf.thunderwasm.runtime.linker.function;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
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
     * Simple implementation of {@link LinkedFunction}.
     * <p>
     * Used by the default function generator for referencing
     * functions.
     */
    class Simple implements LinkedFunction {
        private final MethodHandle methodHandle;
        private final List<ValueType> arguments;
        private final List<ValueType> returnTypes;

        public Simple(
                MethodHandle methodHandle,
                List<ValueType> arguments,
                List<ValueType> returnTypes
        ) {
            this.methodHandle = methodHandle;
            this.arguments = arguments;
            this.returnTypes = returnTypes;
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

        /**
         * Infers a {@link Simple} from a {@link MethodHandle}.
         *
         * @param methodHandle the method handle to infer from
         * @return a new {@link Simple} instance
         * @throws WasmAssemblerException if the method handle's types cannot be converted
         */
        public static Simple inferFromMethodHandle(
                MethodHandle methodHandle
        ) throws WasmAssemblerException {
            MethodType type = methodHandle.type();

            Class<?>[] argumentClasses = type.parameterArray();
            Class<?> returnClass = type.returnType();

            List<ValueType> argumentTypes = new ArrayList<>(argumentClasses.length);
            for (Class<?> argumentClass : argumentClasses) {
                argumentTypes.add(WasmTypeConverter.fromJavaType(JavaType.of(argumentClass)));
            }

            JavaType javaReturnType = JavaType.of(returnClass);
            List<ValueType> returnTypes;
            if (javaReturnType.equals(PrimitiveType.VOID)) {
                returnTypes = Collections.emptyList();
            } else {
                returnTypes = Collections.singletonList(WasmTypeConverter.fromJavaType(javaReturnType));
            }

            return new Simple(methodHandle, argumentTypes, returnTypes);
        }
    }
}
