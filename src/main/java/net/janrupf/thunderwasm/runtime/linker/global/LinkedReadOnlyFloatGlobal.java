package net.janrupf.thunderwasm.runtime.linker.global;

import java.lang.invoke.MethodHandle;

public interface LinkedReadOnlyFloatGlobal extends LinkedGlobal {
    float get();

    class Handle implements LinkedReadOnlyFloatGlobal {
        private final MethodHandle get;

        public Handle(MethodHandle get) {
            this.get = get;
        }

        @Override
        public float get() {
            try {
                return (float) this.get.invokeExact();
            } catch (Throwable t) {
                throw new RuntimeException("Failed to get float global by handle", t);
            }
        }
    }
}
