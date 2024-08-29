package net.janrupf.thunderwasm.module.encoding;

import java.util.Objects;

/**
 * Helper for indexing into large arrays.
 */
public class LargeArrayIndex implements Comparable<LargeArrayIndex> {
    public static final LargeArrayIndex ZERO = new LargeArrayIndex(0, 0);

    private final int arrayIndex;
    private final int elementIndex;

    /**
     * Creates a new large array index.
     *
     * @param arrayIndex the index of the array
     * @param elementIndex the index of the element in the array
     */
    public LargeArrayIndex(int arrayIndex, int elementIndex) {
        this.arrayIndex = arrayIndex;
        this.elementIndex = elementIndex;
    }

    /**
     * Retrieves the index of the array.
     *
     * @return the index of the array
     */
    public int getArrayIndex() {
        return arrayIndex;
    }

    /**
     * Retrieves the index of the element in the array.
     *
     * @return the index of the element in the array
     */
    public int getElementIndex() {
        return elementIndex;
    }

    /**
     * Converts the large array index to an unsigned 64-bit index.
     *
     * @return the unsigned 64-bit index
     */
    public long toU64() {
        return ((long) arrayIndex) * ((long) Integer.MAX_VALUE) + ((long) elementIndex);
    }

    /**
     * Add an offset to this large array index.
     *
     * @param offset the offset to add
     * @return the new large array index
     */
    public LargeArrayIndex add(long offset) {
        long result = toU64() + offset;
        return fromU64(result);
    }

    /**
     * Add an offset to this large array index.
     *
     * @param offset the offset to add
     * @return the new large array index
     */
    public LargeArrayIndex add(LargeArrayIndex offset) {
        long result = toU64() + offset.toU64();
        return fromU64(result);
    }

    /**
     * Subtract an offset from this large array index.
     *
     * @param offset the offset to subtract
     * @return the new large array index
     */
    public LargeArrayIndex subtract(long offset) {
        long result = toU64() - offset;
        return fromU64(result);
    }

    /**
     * Subtract an offset from this large array index.
     *
     * @param offset the offset to subtract
     * @return the new large array index
     */
    public LargeArrayIndex subtract(LargeArrayIndex offset) {
        long result = toU64() - offset.toU64();
        return fromU64(result);
    }

    /**
     * Converts an unsigned 64-bit index to a large array index.
     *
     * @param index the index to convert
     * @return the large array index
     */
    public static LargeArrayIndex fromU64(long index) {
        long arrayIndex = Long.divideUnsigned(index, Integer.MAX_VALUE);
        if (arrayIndex < 0 || arrayIndex > Integer.MAX_VALUE) {
            throw new ArrayIndexOutOfBoundsException("Index out of bounds: " + Long.toUnsignedString(index));
        }

        long elementIndex = Long.remainderUnsigned(index, Integer.MAX_VALUE);
        return new LargeArrayIndex((int) arrayIndex, (int) elementIndex);
    }

    /**
     * Converts an unsigned 32-bit index to a large array index.
     *
     * @param index the index to convert
     * @return the large array index
     */
    public static LargeArrayIndex fromU32(int index) {
        return fromU64(index & 0xFFFFFFFFL);
    }

    @Override
    public String toString() {
        return Long.toUnsignedString(toU64());
    }

    @Override
    public int compareTo(LargeArrayIndex o) {
        return Long.compareUnsigned(toU64(), o.toU64());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LargeArrayIndex)) return false;
        LargeArrayIndex that = (LargeArrayIndex) o;
        return arrayIndex == that.arrayIndex && elementIndex == that.elementIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(arrayIndex, elementIndex);
    }
}
