package net.janrupf.thunderwasm.types;

import net.janrupf.thunderwasm.data.Limits;

/**
 * Represents a memory type.
 */
public final class MemoryType {
    private final Limits limits;

    public MemoryType(Limits limits) {
        this.limits = limits;
    }

    /**
     * Retrieves the limits of the memory.
     *
     * @return the limits
     */
    public Limits getLimits() {
        return limits;
    }
}
