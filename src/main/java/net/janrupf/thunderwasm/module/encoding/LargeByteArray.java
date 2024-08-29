package net.janrupf.thunderwasm.module.encoding;

import java.util.Arrays;
import java.util.Objects;

/**
 * Byte array which potentially holds up huge amounts of data.
 */
public class LargeByteArray {
    private final byte[][] data;

    /**
     * Creates a new large byte array.
     *
     * @param length the length of the array
     */
    public LargeByteArray(LargeArrayIndex length) {
        if (length.equals(LargeArrayIndex.ZERO)) {
            // Short circuit: array is empty
            this.data = new byte[0][];
            return;
        }

        int arrayCount = length.getArrayIndex() + 1;
        this.data = new byte[arrayCount][];

        int lastArraySize = length.getElementIndex();
        if (lastArraySize == 0) {
            arrayCount--;
            lastArraySize = Integer.MAX_VALUE;
        }

        for (int i = 0; i < arrayCount - 1; i++) {
            this.data[i] = new byte[Integer.MAX_VALUE];
        }

        this.data[arrayCount - 1] = new byte[lastArraySize];
    }

    /**
     * Retrieves the length of the array.
     *
     * @param index the index to get the value from
     * @return the value at the index
     */
    public byte get(LargeArrayIndex index) {
        try {
            return data[index.getArrayIndex()][index.getElementIndex()];
        } catch (ArrayIndexOutOfBoundsException e) {
            ArrayIndexOutOfBoundsException n = new ArrayIndexOutOfBoundsException(
                    "Large array index out of bounds: " + Long.toUnsignedString(index.toU64())
            );
            n.addSuppressed(e);
            throw n;
        }
    }

    /**
     * Sets a value in the array.
     *
     * @param index the index to set the value at
     * @param value the value to set
     */
    public void set(LargeArrayIndex index, byte value) {
        try {
            data[index.getArrayIndex()][index.getElementIndex()] = value;
        } catch (ArrayIndexOutOfBoundsException e) {
            ArrayIndexOutOfBoundsException n = new ArrayIndexOutOfBoundsException(
                    "Large array index out of bounds: " + Long.toUnsignedString(index.toU64())
            );
            n.addSuppressed(e);
            throw n;
        }
    }

    /**
     * Sets a region in the array.
     *
     * @param index the index to set the region at
     * @param region the region to set
     */
    public void setRegion(LargeArrayIndex index, byte[] region) {
        int arrayIndex = index.getArrayIndex();
        int elementIndex = index.getElementIndex();

        int regionIndex = 0;
        while (regionIndex < region.length) {
            int remaining = region.length - regionIndex;
            int copyLength = Math.min(remaining, Integer.MAX_VALUE - elementIndex);

            System.arraycopy(region, regionIndex, data[arrayIndex], elementIndex, copyLength);

            regionIndex += copyLength;
            arrayIndex++;
            elementIndex = 0;
        }
    }

    /**
     * Determines whether the given index is valid for this array.
     *
     * @param index the index to check
     * @return true if the index is valid, false otherwise
     */
    public boolean isValid(LargeArrayIndex index) {
        return index.getArrayIndex() < data.length && index.getElementIndex() < data[index.getArrayIndex()].length;
    }

    /**
     * Retrieves the length of the array as a {@link LargeArrayIndex}.
     *
     * @return the length of the array
     */
    public LargeArrayIndex largeLength() {
        return LargeArrayIndex.fromU64(length());
    }

    /**
     * Retrieves the length of the array.
     *
     * @return the length of the array
     */
    public long length() {
        if (data.length == 0) {
            return 0;
        }

        return ((long) (data.length - 1)) * ((long) Integer.MAX_VALUE) + ((long) data[data.length - 1].length);
    }

    /**
     * Converts the large byte array to a flat byte array.
     *
     * @return the flat byte array, or null, if this array is too large
     */
    public byte[] asFlatArray() {
        switch (data.length) {
            case 0:
                return new byte[0];
            case 1:
                return data[0];
            default:
                return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LargeByteArray)) return false;
        LargeByteArray that = (LargeByteArray) o;
        return Objects.deepEquals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(data);
    }
}
