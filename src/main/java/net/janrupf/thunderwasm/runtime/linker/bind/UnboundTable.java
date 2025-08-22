package net.janrupf.thunderwasm.runtime.linker.bind;

import java.lang.invoke.MethodHandle;

final class UnboundTable {
    private final MethodHandle getter;
    private final boolean isStatic;

    private UnboundTable(MethodHandle getter, boolean isStatic) {
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
