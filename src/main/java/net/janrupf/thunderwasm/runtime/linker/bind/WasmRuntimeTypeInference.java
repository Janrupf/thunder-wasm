package net.janrupf.thunderwasm.runtime.linker.bind;

import net.janrupf.thunderwasm.runtime.continuation.Continuation;
import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper class for inferring WebAssembly types from Java classes.
 */
public final class WasmRuntimeTypeInference {
    private WasmRuntimeTypeInference() {
        throw new AssertionError("No instances of WasmRuntimeTypeInference are allowed");
    }

    /**
     * Infer the appropriate {@link ValueType} for a given Java class.
     *
     * @param t the class to infer the type for
     * @return the inferred {@link ValueType}
     */
    public static ValueType inferWasmType(Class<?> t) {
        if (t == boolean.class || t == Boolean.class ||
                t == byte.class || t == Byte.class ||
                t == short.class || t == Short.class ||
                t == int.class || t == Integer.class) {
            return NumberType.I32;
        } else if (t == long.class || t == Long.class) {
            return NumberType.I64;
        } else if (t == float.class || t == Float.class) {
            return NumberType.F32;
        } else if (t == double.class || t == Double.class) {
            return NumberType.F64;
        }

        if (LinkedFunction.class.isAssignableFrom(t)) {
            return ReferenceType.FUNCREF;
        }

        return ReferenceType.EXTERNREF;
    }

    /**
     * Infer the appropriate {@link ValueType}s for an array of Java classes.
     *
     * @param types the array of classes to infer the types for
     * @return a list of inferred {@link ValueType}s
     */
    public static List<ValueType> inferWasmTypes(Class<?>[] types) {
        List<ValueType> result = new ArrayList<>(types.length);
        for (Class<?> t : types) {
            result.add(inferWasmType(t));
        }
        return result;
    }

    /**
     * Infer the appropriate {@link ValueType}s for the return type of a Java method.
     *
     * @param returnType the return type of the method
     * @return a list of inferred {@link ValueType}s
     */
    public static List<ValueType> inferWasmReturnType(Class<?> returnType) {
        if (returnType == void.class || returnType == Void.class) {
            return Collections.emptyList();
        }
        return Collections.singletonList(inferWasmType(returnType));
    }

    /**
     * Infer the method signature for a given method based on its parameter types and return type.
     *
     * @param parameterTypes the parameter types of the method
     * @param returnType     the return type of the method
     * @return the inferred {@link MethodSignature}
     */
    public static MethodSignature inferMethodSignature(
            Class<?>[] parameterTypes,
            Class<?> returnType
    ) {
        int continuationArgumentIndex = -1;
        List<ValueType> wasmParameterTypes = new ArrayList<>();

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> paramType = parameterTypes[i];

            if (paramType.equals(Continuation.class) && continuationArgumentIndex == -1) {
                continuationArgumentIndex = i;
                continue;
            }

            wasmParameterTypes.add(inferWasmType(paramType));
        }

        List<ValueType> wasmReturnTypes = inferWasmReturnType(returnType);
        return new MethodSignature(wasmParameterTypes, wasmReturnTypes, continuationArgumentIndex);
    }

    /**
     * Method signature representation for a WebAssembly function with Java quirks.
     */
    public static final class MethodSignature {
        private final List<ValueType> parameterTypes;
        private final List<ValueType> returnTypes;
        private final int continuationArgumentIndex;

        public MethodSignature(List<ValueType> parameterTypes, List<ValueType> returnTypes, int continuationArgumentIndex) {
            this.parameterTypes = Collections.unmodifiableList(parameterTypes);
            this.returnTypes = Collections.unmodifiableList(returnTypes);
            this.continuationArgumentIndex = continuationArgumentIndex;
        }

        /**
         * Retrieve the WASM parameter types for this method signature.
         * <p>
         * The continuation argument, if present, is not included in this list.
         *
         * @return an unmodifiable list of {@link ValueType} representing the parameter types
         */
        public List<ValueType> getParameterTypes() {
            return parameterTypes;
        }

        /**
         * Retrieve the WASM return types for this method signature.
         *
         * @return an unmodifiable list of {@link ValueType} representing the return types
         */
        public List<ValueType> getReturnTypes() {
            return returnTypes;
        }

        /**
         * Get the index of the continuation argument in the parameter list, if present.
         *
         * @return the index of the continuation argument, or -1 if not present
         */
        public int getContinuationArgumentIndex() {
            return continuationArgumentIndex;
        }
    }
}
