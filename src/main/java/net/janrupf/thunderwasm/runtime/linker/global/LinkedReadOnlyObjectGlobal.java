package net.janrupf.thunderwasm.runtime.linker.global;

import net.janrupf.thunderwasm.runtime.ExternReference;
import net.janrupf.thunderwasm.runtime.FunctionReference;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;

import java.lang.invoke.MethodHandle;

public interface LinkedReadOnlyObjectGlobal<T> extends LinkedReadOnlyGlobal {
    T get();

    class Handle<T> implements LinkedReadOnlyObjectGlobal<T> {
        private final MethodHandle get;

        public Handle(MethodHandle get) {
            this.get = get;
        }

        @Override
        public ValueType getType() {
            Class<?> getType = get.type().returnType();

            if (getType == FunctionReference.class) {
                return ReferenceType.FUNCREF;
            } else if (getType == ExternReference.class) {
                return ReferenceType.EXTERNREF;
            } else {
                throw new IllegalStateException("Object global type is not convertible to a WASM type");
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get() {
            try {
                return (T) this.get.invoke();
            } catch (Throwable t) {
                throw new RuntimeException("Failed to get object global by handle", t);
            }
        }
    }
}
