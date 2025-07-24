package net.janrupf.thunderwasm.runtime.linker.global;

import java.lang.invoke.MethodHandle;

public interface LinkedDoubleGlobal extends LinkedReadOnlyDoubleGlobal {
    void set(double value);

    class Handle extends LinkedReadOnlyDoubleGlobal.Handle implements LinkedDoubleGlobal {
        private final MethodHandle set;

        public Handle(MethodHandle get, MethodHandle set) {
            super(get);
            this.set = set;
        }

        @Override
        public void set(double value) {
            try {
                set.invokeExact(value);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to set double global by handle", t);
            }
        }
    }
}
