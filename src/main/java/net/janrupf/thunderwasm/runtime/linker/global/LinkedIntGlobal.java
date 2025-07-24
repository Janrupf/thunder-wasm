package net.janrupf.thunderwasm.runtime.linker.global;

import java.lang.invoke.MethodHandle;

public interface LinkedIntGlobal extends LinkedReadOnlyIntGlobal {
    void set(int value);

    class Handle extends LinkedReadOnlyIntGlobal.Handle implements LinkedIntGlobal {
        private final MethodHandle set;

        public Handle(MethodHandle get, MethodHandle set) {
            super(get);
            this.set = set;
        }

        @Override
        public void set(int value) {
            try {
                set.invokeExact(value);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to set int global by handle", t);
            }
        }
    }
}
