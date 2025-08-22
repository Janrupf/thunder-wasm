package net.janrupf.thunderwasm.runtime.linker.bind;

import java.lang.invoke.MethodHandle;

final class UnboundMemory {
    private final MethodHandle getter;
    private final boolean isStatic;

    private UnboundMemory(MethodHandle getter, boolean isStatic) {
        this.getter = getter;
        this.isStatic = isStatic;
    }

    public MethodHandle getGetter() {
        return getter;
    }

    public boolean isStatic() {
        return isStatic;
    }
}
