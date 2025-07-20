package net.janrupf.thunderwasm.instructions.memory.base;

import net.janrupf.thunderwasm.types.NumberType;

public abstract class PlainMemoryLoad extends PlainMemory {
    private final LoadType loadType;

    protected PlainMemoryLoad(String name, byte opCode, NumberType numberType, LoadType loadType) {
        super(name, opCode, numberType);
        this.loadType = loadType;
    }

    public final LoadType getLoadType() {
        return loadType;
    }

    /**
     * Describes how the memory is loaded.
     */
    public enum LoadType {
        /**
         * Native bit width load. Bit width depends on the data type.
         */
        NATIVE,

        /**
         * Signed 8 bit load.
         */
        SIGNED_8,

        /**
         * Unsigned 8 bit load.
         */
        UNSIGNED_8,

        /**
         * Signed 16 bit load.
         */
        SIGNED_16,

        /**
         * Unsigned 16 bit load.
         */
        UNSIGNED_16,

        /**
         * Signed 32 bit load.
         */
        SIGNED_32,

        /**
         * Unsigned 32 bit load.
         */
        UNSIGNED_32,
    }
}
