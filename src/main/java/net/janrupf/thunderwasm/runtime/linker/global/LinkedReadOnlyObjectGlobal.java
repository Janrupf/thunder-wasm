package net.janrupf.thunderwasm.runtime.linker.global;

import net.janrupf.thunderwasm.types.ValueType;

import java.lang.invoke.MethodHandle;

public interface LinkedReadOnlyObjectGlobal<T> extends LinkedReadOnlyGlobal {
    T get();

    class Handle<T> implements LinkedReadOnlyObjectGlobal<T> {
        private final ValueType type;
        private final MethodHandle get;

        public Handle(ValueType type, MethodHandle get) {
            this.type = type;
            this.get = get;
        }

        @Override
        public ValueType getType() {
            return type;
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
