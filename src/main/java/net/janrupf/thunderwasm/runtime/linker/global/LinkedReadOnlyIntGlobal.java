package net.janrupf.thunderwasm.runtime.linker.global;

import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ValueType;

import java.lang.invoke.MethodHandle;

public interface LinkedReadOnlyIntGlobal extends LinkedReadOnlyGlobal {
    int get();

    @Override
    default ValueType getType() {
        return NumberType.I32;
    }

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
