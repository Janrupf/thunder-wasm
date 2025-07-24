package net.janrupf.thunderwasm.runtime.linker.global;

import java.lang.invoke.MethodHandle;

public interface LinkedLongGlobal extends LinkedReadOnlyLongGlobal {
    void set(long value);

    class Handle extends LinkedReadOnlyLongGlobal.Handle implements LinkedLongGlobal {
        private final MethodHandle set;

        public Handle(MethodHandle get, MethodHandle set) {
            super(get);
            this.set = set;
        }

        @Override
        public void set(long value) {
            try {
                set.invokeExact(value);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to set long global by handle", t);
            }
        }
    }
}
