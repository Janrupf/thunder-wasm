package net.janrupf.thunderwasm.runtime.linker.bind;

import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;
import net.janrupf.thunderwasm.types.ValueType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

final class UnboundFunction implements BindableExport<LinkedFunction> {
    private final MethodHandle methodHandle;
    private final List<ValueType> parameterTypes;
    private final List<ValueType> returnTypes;
    private final int continuationArgumentIndex;
    private final boolean isStatic;

    public static UnboundFunction of(MethodHandles.Lookup lookup, Method m) throws ReflectiveRuntimeLinkerException {
        WasmRuntimeTypeInference.MethodSignature signature = WasmRuntimeTypeInference.inferMethodSignature(
                m.getParameterTypes(),
                m.getReturnType()
        );

        return new UnboundFunction(
                ReflectiveRuntimeLinkerException.catching(() -> lookup.unreflect(m)),
                signature.getParameterTypes(),
                signature.getReturnTypes(),
                signature.getContinuationArgumentIndex(),
                Modifier.isStatic(m.getModifiers())
        );
    }

    public static UnboundFunction of(MethodHandles.Lookup lookup, Constructor<?> c) throws ReflectiveRuntimeLinkerException {
        WasmRuntimeTypeInference.MethodSignature signature = WasmRuntimeTypeInference.inferMethodSignature(
                c.getParameterTypes(),
                c.getDeclaringClass()
        );

        return new UnboundFunction(
                ReflectiveRuntimeLinkerException.catching(() -> lookup.unreflectConstructor(c)),
                signature.getParameterTypes(),
                signature.getReturnTypes(),
                signature.getContinuationArgumentIndex(),
                true
        );
    }

    public UnboundFunction(
            MethodHandle methodHandle,
            List<ValueType> parameterTypes,
            List<ValueType> returnTypes,
            int continuationArgumentIndex,
            boolean isStatic
    ) {
        this.methodHandle = methodHandle;
        this.parameterTypes = parameterTypes;
        this.returnTypes = returnTypes;
        this.continuationArgumentIndex = continuationArgumentIndex;
        this.isStatic = isStatic;
    }

    @Override
    public LinkedFunction bind(Object instance) throws ReflectiveRuntimeLinkerException {
        MethodHandle bound = BindableExport.bindHandle(isStatic, instance, methodHandle);
        return new Bound(bound);
    }

    private final class Bound implements LinkedFunction {
        private final MethodHandle bound;

        private Bound(MethodHandle bound) {
            this.bound = bound;
        }

        @Override
        public MethodHandle asMethodHandle() {
            return bound;
        }

        @Override
        public List<ValueType> getArguments() {
            return parameterTypes;
        }

        @Override
        public List<ValueType> getReturnTypes() {
            return returnTypes;
        }

        @Override
        public int getContinuationArgumentIndex() {
            return continuationArgumentIndex;
        }
    }
}
