package net.janrupf.thunderwasm.runtime.linker.global;

public interface LinkedReadOnlyObjectGlobal<T> extends LinkedGlobal {
    T get();
}
