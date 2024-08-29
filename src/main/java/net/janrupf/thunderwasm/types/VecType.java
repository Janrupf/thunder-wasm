package net.janrupf.thunderwasm.types;

public final class VecType extends ValueType {
    private final int bitWidth;

    private VecType(String name, int bitWidth) {
        super(name);
        this.bitWidth = bitWidth;
    }

    /**
     * Retrieves the bit width of the vector type.
     *
     * @return the bit width of the vector type
     */
    public int getBitWidth() {
        return bitWidth;
    }

    /**
     * Represents a 128-bit vector.
     */
    public static final VecType V128 = new VecType("v128", 128);
}
