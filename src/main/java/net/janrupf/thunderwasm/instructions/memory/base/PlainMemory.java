package net.janrupf.thunderwasm.instructions.memory.base;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.NumberType;

import java.io.IOException;

public abstract class PlainMemory extends WasmInstruction<PlainMemory.Memarg> {
    private final NumberType numberType;

    protected PlainMemory(String name, byte opCode, NumberType numberType) {
        super(name, opCode);
        this.numberType = numberType;
    }

    @Override
    public Memarg readData(WasmLoader loader) throws IOException, InvalidModuleException {
        Memarg arg = Memarg.read(loader);

        if (arg.getAlignment() > 31) {
            throw new InvalidModuleException("Alignment exponent must not be greater than 31, got " + arg.getAlignment());
        }

        return arg;
    }

    public final void validate(Memarg memarg, int bitWidth) throws WasmAssemblerException {
        if (bitWidth == -1) {
            bitWidth = getNumberType().getBitWidth();
        }

        int requestedAlignment = 1 << memarg.getAlignment();
        int naturalAlignment = bitWidth / 8;

        if (Integer.compareUnsigned(requestedAlignment, naturalAlignment) > 0) {
            throw new WasmAssemblerException("Requested alignment " + Integer.toUnsignedString(requestedAlignment) +
                    " is greater than natural alignment " + naturalAlignment +
                    " for memory operation of type " + getNumberType() + " and load of " + bitWidth + " bits");
        }
    }

    public final NumberType getNumberType() {
        return numberType;
    }

    public static final class Memarg implements WasmInstruction.Data {
        private final int alignment;
        private final int offset;

        public Memarg(int alignment, int offset) {
            this.alignment = alignment;
            this.offset = offset;
        }

        /**
         * Get the alignment of this memarg.
         * <p>
         * This is the exponent of a power of 2.
         * In other words, this function returns `n` for `2^n`.
         *
         * @return the alignment
         */
        public int getAlignment() {
            return alignment;
        }

        /**
         * Get the base offset of this memarg.
         *
         * @return the base offset of this memarg
         */
        public int getOffset() {
            return offset;
        }

        @Override
        public String toString() {
            return "alignment=" + this.alignment + " offset=" + this.offset;
        }

        /**
         * Reads a {@link Memarg} from a {@link WasmLoader}.
         *
         * @param loader the loader to read from
         * @return the read memarg
         * @throws IOException if an I/O error occurs
         * @throws InvalidModuleException if invalid data is encountered
         */
        public static Memarg read(WasmLoader loader) throws IOException, InvalidModuleException {
            int alignment = loader.readU32();
            int offset = loader.readU32();

            return new Memarg(alignment, offset);
        }
    }
}
