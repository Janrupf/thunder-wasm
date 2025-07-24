package net.janrupf.thunderwasm.runtime.linker.global;

import java.lang.invoke.MethodHandle;

public interface LinkedReadOnlyLongGlobal extends LinkedGlobal {
    long get();

    class Handle implements LinkedReadOnlyLongGlobal {
        private final MethodHandle get;

        public Handle(MethodHandle get) {
            this.get = get;
        }

        @Override
        public long get() {
            try {
                return (long) this.get.invokeExact();
            } catch (Throwable t) {
                throw new RuntimeException("Failed to get long global by handle", t);
            }
        }
    }
}
