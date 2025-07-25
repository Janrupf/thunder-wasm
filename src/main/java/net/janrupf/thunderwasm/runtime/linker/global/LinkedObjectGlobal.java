package net.janrupf.thunderwasm.runtime.linker.global;

import net.janrupf.thunderwasm.types.ValueType;

import java.lang.invoke.MethodHandle;

public interface LinkedObjectGlobal<T> extends LinkedReadOnlyObjectGlobal<T>, LinkedGlobal {
    void set(T value);

    class Handle<T> extends LinkedReadOnlyObjectGlobal.Handle<T> implements LinkedObjectGlobal<T> {
        private final MethodHandle set;

        public Handle(ValueType type, MethodHandle get, MethodHandle set) {
            super(type, get);
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
