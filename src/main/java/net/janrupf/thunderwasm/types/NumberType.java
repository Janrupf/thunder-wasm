package net.janrupf.thunderwasm.types;

import java.util.Objects;

public final class NumberType extends ValueType {
    private final int bitWidth;
    private final Number minValue;
    private final Number maxValue;

    private NumberType(String name, int bitWidth, Number minValue, Number maxValue) {
        super(name);
        this.bitWidth = bitWidth;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * Retrieves the bit width of the number type.
     *
     * @return the bit width of the number type
     */
    public int getBitWidth() {
        return bitWidth;
    }

    /**
     * Represents a 32-bit integer.
     */
    public static final NumberType I32 = new NumberType("i32", 32, Integer.MIN_VALUE, Integer.MAX_VALUE);

    /**
     * Represents a 64-bit integer.
     */
    public static final NumberType I64 = new NumberType("i64", 64, Long.MIN_VALUE, Long.MAX_VALUE);

    /**
     * Represents a 32-bit floating point number.
     */
    public static final NumberType F32 = new NumberType("f32", 32, Float.MIN_VALUE, Float.MAX_VALUE);

    /**
     * Represents a 64-bit floating point number.
     */
    public static final NumberType F64 = new NumberType("f64", 64, Double.MIN_VALUE, Double.MAX_VALUE);

    public Number getMinValue() {
        return minValue;
    }

    public Number getMaxValue() {
        return maxValue;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NumberType)) return false;
        if (!super.equals(o)) return false;
        NumberType that = (NumberType) o;
        return bitWidth == that.bitWidth;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), bitWidth);
    }
}
