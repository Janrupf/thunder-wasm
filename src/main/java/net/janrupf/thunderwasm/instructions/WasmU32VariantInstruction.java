package net.janrupf.thunderwasm.instructions;

import net.janrupf.thunderwasm.instructions.decoder.InstructionDecoder;

import java.util.Objects;

public abstract class WasmU32VariantInstruction<D extends WasmInstruction.Data> extends WasmInstruction<D> {
    private final int variant;

    public WasmU32VariantInstruction(String name, byte opCode, int variant) {
        super(name, opCode);
        this.variant = variant;
    }

    @Override
    public final InstructionDecoder getDecoder() {
        return InstructionDecoder.u32Variant(variant, this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WasmU32VariantInstruction)) return false;
        if (!super.equals(o)) return false;
        WasmU32VariantInstruction<?> that = (WasmU32VariantInstruction<?>) o;
        return variant == that.variant;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), variant);
    }
}
