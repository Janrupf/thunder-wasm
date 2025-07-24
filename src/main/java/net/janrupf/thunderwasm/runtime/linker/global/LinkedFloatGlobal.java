package net.janrupf.thunderwasm.runtime.linker.global;

import java.lang.invoke.MethodHandle;

public interface LinkedFloatGlobal extends LinkedReadOnlyFloatGlobal {
    void set(float value);

    class Handle extends LinkedReadOnlyFloatGlobal.Handle implements LinkedFloatGlobal {
        private final MethodHandle set;

        public Handle(MethodHandle get, MethodHandle set) {
            super(get);
            this.set = set;
        }

        @Override
        public void set(float value) {
            try {
                set.invokeExact(value);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to set float global by handle", t);
            }
        }
    }
}
