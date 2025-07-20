package net.janrupf.thunderwasm.instructions.memory.base;

import net.janrupf.thunderwasm.types.NumberType;

public abstract class PlainMemoryStore extends PlainMemory {
    private final StoreType storeType;

    protected PlainMemoryStore(String name, byte opCode, NumberType numberType, StoreType storeType) {
        super(name, opCode, numberType);
        this.storeType = storeType;
    }

    public final StoreType getStoreType() {
        return storeType;
    }

    /**
     * Describes how the memory is loaded.
     */
    public enum StoreType {
        /**
         * Native bit width load. Bit width depends on the data type.
         */
        NATIVE,

        /**
         * 8 bit store.
         */
        BIT_8,

        /**
         * 16 bit store.
         */
        BIT_16,

        /**
         * 32 bit store.
         */
        BIT_32,
    }
}
