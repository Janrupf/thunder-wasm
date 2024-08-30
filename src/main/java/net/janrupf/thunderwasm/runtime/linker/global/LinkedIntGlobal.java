package net.janrupf.thunderwasm.runtime.linker.global;

public interface LinkedIntGlobal extends LinkedReadOnlyIntGlobal {
    void set(int value);
}
