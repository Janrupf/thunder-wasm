package net.janrupf.thunderwasm.instructions.memory.base;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.imports.MemoryImportDescription;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.MemoryType;
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

    @Override
    public void emitCode(CodeEmitContext context, Memarg data) throws WasmAssemblerException {
        validate(data, storeType.getBitWidth());

        context.getFrameState().popOperand(getNumberType());
        context.getFrameState().popOperand(NumberType.I32);

        FoundElement<MemoryType, MemoryImportDescription> memoryElement = context.getLookups().requireMemory(LargeArrayIndex.ZERO);

        if (memoryElement.isImport()) {
            context.getGenerators().getImportGenerator().emitMemoryStore(
                    memoryElement.getImport(),
                    getNumberType(),
                    data,
                    getStoreType(),
                    context
            );
        } else {
            context.getGenerators().getMemoryGenerator().emitStore(
                    LargeArrayIndex.ZERO,
                    memoryElement.getElement(),
                    getNumberType(),
                    data,
                    getStoreType(),
                    context
            );
        }
    }

    /**
     * Describes how the memory is loaded.
     */
    public enum StoreType {
        /**
         * Native bit width load. Bit width depends on the data type.
         */
        NATIVE(-1),

        /**
         * 8 bit store.
         */
        BIT_8(8),

        /**
         * 16 bit store.
         */
        BIT_16(16),

        /**
         * 32 bit store.
         */
        BIT_32(32);

        private final int bitWidth;

        StoreType(int bitWidth) {
            this.bitWidth = bitWidth;
        }

        /**
         * Retrieves the bit width of this store type.
         * <p>
         * If the store type is {@link #NATIVE}, this function returns -1.
         *
         * @return the bit width of this store type
         */
        public int getBitWidth() {
            return bitWidth;
        }
    }
}
