package net.janrupf.thunderwasm.instructions.memory.base;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.imports.MemoryImportDescription;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.MemoryType;
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

    @Override
    public void emitCode(CodeEmitContext context, Memarg data) throws WasmAssemblerException {
        validate(data, loadType.getBitWidth());

        context.getFrameState().popOperand(NumberType.I32);

        FoundElement<MemoryType, MemoryImportDescription> memoryElement = context.getLookups().requireMemory(LargeArrayIndex.ZERO);

        if (memoryElement.isImport()) {
            context.getGenerators().getImportGenerator().emitMemoryLoad(
                    memoryElement.getImport(),
                    getNumberType(),
                    data,
                    getLoadType(),
                    context
            );
        } else {
            context.getGenerators().getMemoryGenerator().emitLoad(
                    LargeArrayIndex.ZERO,
                    memoryElement.getElement(),
                    getNumberType(),
                    data,
                    getLoadType(),
                    context
            );
        }

        context.getFrameState().pushOperand(getNumberType());
    }

    /**
     * Describes how the memory is loaded.
     */
    public enum LoadType {
        /**
         * Native bit width load. Bit width depends on the data type.
         */
        NATIVE(true /* in Java everything is signed - sign extension is the default (not that it matters for NATIVE) */, -1),

        /**
         * Signed 8 bit load.
         */
        SIGNED_8(true, 8),

        /**
         * Unsigned 8 bit load.
         */
        UNSIGNED_8(false, 8),

        /**
         * Signed 16 bit load.
         */
        SIGNED_16(true, 16),

        /**
         * Unsigned 16 bit load.
         */
        UNSIGNED_16(false, 16),

        /**
         * Signed 32 bit load.
         */
        SIGNED_32(true, 32),

        /**
         * Unsigned 32 bit load.
         */
        UNSIGNED_32(false, 32);

        private final boolean signed;
        private final int bitWidth;

        LoadType(boolean signed, int bitWidth) {
            this.signed = signed;
            this.bitWidth = bitWidth;
        }

        /**
         * Check if this load type is signed.
         *
         * @return true if this load type is signed, false otherwise
         */
        public final boolean isSigned() {
            return signed;
        }

        /**
         * Retrieves the bit width of this load type.
         * <p>
         * If the load type is {@link #NATIVE}, this function returns -1.
         *
         * @return the bit width of this load type
         */
        public final int getBitWidth() {
            return bitWidth;
        }
    }
}
