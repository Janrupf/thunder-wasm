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
        FoundElement<MemoryType, MemoryImportDescription> memoryElement = context.getLookups().requireMemory(LargeArrayIndex.ZERO);

        if (memoryElement.isImport()) {
            throw new WasmAssemblerException("Loads from imported memory are not supported yet");
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
    }

    /**
     * Describes how the memory is loaded.
     */
    public enum LoadType {
        /**
         * Native bit width load. Bit width depends on the data type.
         */
        NATIVE(true /* in Java everything is signed - sign extension is the default (not that it matters for NATIVE) */),

        /**
         * Signed 8 bit load.
         */
        SIGNED_8(true),

        /**
         * Unsigned 8 bit load.
         */
        UNSIGNED_8(false),

        /**
         * Signed 16 bit load.
         */
        SIGNED_16(true),

        /**
         * Unsigned 16 bit load.
         */
        UNSIGNED_16(false),

        /**
         * Signed 32 bit load.
         */
        SIGNED_32(true),

        /**
         * Unsigned 32 bit load.
         */
        UNSIGNED_32(false);

        private final boolean signed;

        LoadType(boolean signed) {
            this.signed = signed;
        }

        /**
         * Check if this load type is signed.
         *
         * @return true if this load type is signed, false otherwise
         */
        public final boolean isSigned() {
            return signed;
        }
    }
}
