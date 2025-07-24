package net.janrupf.thunderwasm.runtime.linker.global;

import java.lang.invoke.MethodHandle;

public interface LinkedReadOnlyObjectGlobal<T> extends LinkedGlobal {
    T get();

    class Handle<T> implements LinkedReadOnlyObjectGlobal<T> {
        private final MethodHandle get;

        public Handle(MethodHandle get) {
            this.get = get;
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
