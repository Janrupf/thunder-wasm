package net.janrupf.thunderwasm.runtime.linker.global;

import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ValueType;

import java.lang.invoke.MethodHandle;

public interface LinkedReadOnlyDoubleGlobal extends LinkedReadOnlyGlobal {
    double get();

    @Override
    default ValueType getType() {
        return NumberType.F64;
    }

    class Handle implements LinkedReadOnlyDoubleGlobal{
        private final MethodHandle get;

        public Handle(MethodHandle get) {
            this.get = get;
        }

        @Override
        public double get() {
            try {
                return (double) this.get.invokeExact();
            } catch (Throwable t) {
                throw new RuntimeException("Failed to get double global by handle", t);
            }
        }
    }
}
