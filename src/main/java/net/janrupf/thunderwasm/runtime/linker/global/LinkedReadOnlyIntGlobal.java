package net.janrupf.thunderwasm.runtime.linker.global;

import java.lang.invoke.MethodHandle;

public interface LinkedReadOnlyIntGlobal extends LinkedGlobal {
    int get();

    class Handle implements LinkedReadOnlyIntGlobal {
        private final MethodHandle get;

        public Handle(MethodHandle get) {
            this.get = get;
        }

        @Override
        public int get() {
            try {
                return (int) this.get.invokeExact();
            } catch (Throwable t) {
                throw new RuntimeException("Failed to get int global by handle", t);
            }
        }
    }
}
