package net.janrupf.thunderwasm.runtime.linker.global;

import java.lang.invoke.MethodHandle;

public interface LinkedObjectGlobal<T> extends LinkedReadOnlyObjectGlobal<T> {
    void set(T value);

    class Handle<T> extends LinkedReadOnlyObjectGlobal.Handle<T> implements LinkedObjectGlobal<T> {
        private final MethodHandle set;

        public Handle(MethodHandle get, MethodHandle set) {
            super(get);
            this.set = set;
        }

        @Override
        public void set(T value) {
            try {
                set.invoke(value);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to set object global by handle", t);
            }
        }
    }
}
