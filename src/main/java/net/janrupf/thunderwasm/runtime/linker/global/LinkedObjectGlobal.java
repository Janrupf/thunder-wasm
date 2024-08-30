package net.janrupf.thunderwasm.runtime.linker.global;

public interface LinkedObjectGlobal<T> extends LinkedReadOnlyObjectGlobal<T> {
    void set(T value);
}
