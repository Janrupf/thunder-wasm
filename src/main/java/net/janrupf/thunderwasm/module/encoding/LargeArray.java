package net.janrupf.thunderwasm.module.encoding;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;

public class LargeArray<T> implements Iterable<T> {
    private final Class<T> valueClass;
    private final T[][] data;

    @SuppressWarnings("unchecked")
    public LargeArray(Class<T> valueClass, LargeArrayIndex length) {
        this.valueClass = valueClass;
        Class<T[]> arrayClass = resolveArrayClass(valueClass);

        if (length.equals(LargeArrayIndex.ZERO)) {
            // Short circuit: array is empty
            this.data = (T[][]) Array.newInstance(arrayClass, 0);
            return;
        }

        int arrayCount = length.getArrayIndex() + 1;
        this.data = (T[][]) Array.newInstance(arrayClass, arrayCount);

        int lastArraySize = length.getElementIndex();
        if (lastArraySize == 0) {
            arrayCount--;
            lastArraySize = Integer.MAX_VALUE;
        }

        for (int i = 0; i < arrayCount - 1; i++) {
            this.data[i] = (T[]) Array.newInstance(valueClass, Integer.MAX_VALUE);
        }

        this.data[arrayCount - 1] = (T[]) Array.newInstance(valueClass, lastArraySize);
    }

    // Optimized constructor for small arrays
    @SuppressWarnings("unchecked")
    private LargeArray(Class<T> valueClass, T[] data) {
        this.valueClass = valueClass;
        this.data = (T[][]) Array.newInstance(resolveArrayClass(valueClass), 1);
        this.data[0] = data;
    }

    /**
     * Retrieves the length of the array.
     *
     * @param index the index to get the value from
     * @return the value at the index
     */
    public T get(LargeArrayIndex index) {
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
    public void set(LargeArrayIndex index, T value) {
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
     * @param index  the index to set the region at
     * @param region the region to set
     */
    public void setRegion(LargeArrayIndex index, T[] region) {
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
    @SuppressWarnings("unchecked")
    public T[] asFlatArray() {
        switch (data.length) {
            case 0:
                return (T[]) Array.newInstance(valueClass, 0);
            case 1:
                return data[0];
            default:
                return null;
        }
    }

    /**
     * Maps the values of this large array to a new large array.
     *
     * @param newValueClass the class of the new values
     * @param mapper        the mapper to apply to the values
     * @param <N>           the type of the new values
     * @return the new large array
     */
    public <N> LargeArray<N> map(Class<N> newValueClass, Function<T, N> mapper) {
        LargeArrayIndex i = LargeArrayIndex.ZERO;
        LargeArray<N> newArray = new LargeArray<>(newValueClass, LargeArrayIndex.fromU64(length()));

        for (T value : this) {
            newArray.set(i, mapper.apply(value));
            i = i.add(1);
        }

        return newArray;
    }

    /**
     * Creates a large array from a flat array.
     *
     * @param valueClass the class of the values
     * @param array      the flat array
     * @param <T>        the type of the values
     * @return the large array
     */
    public static <T> LargeArray<T> fromFlatArray(Class<T> valueClass, T[] array) {
        LargeArrayIndex length = LargeArrayIndex.fromU64(array.length);
        LargeArray<T> largeArray = new LargeArray<>(valueClass, length);
        largeArray.setRegion(LargeArrayIndex.ZERO, array);
        return largeArray;
    }

    /**
     * Creates a large array from a flat array without copying.
     *
     * @param valueClass the class of the values
     * @param values     the flat array
     * @param <T>        the type of the values
     * @return the large array
     */
    @SafeVarargs
    public static <T> LargeArray<T> of(Class<T> valueClass, T... values) {
        return new LargeArray<>(valueClass, values);
    }

    @SuppressWarnings("unchecked")
    private static <X> Class<X[]> resolveArrayClass(Class<X> clazz) {
        return (Class<X[]>) Array.newInstance(clazz, 0).getClass();
    }

    @Override
    public Iterator<T> iterator() {
        return new LargeArrayIterator<>(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LargeArray)) return false;
        LargeArray<?> that = (LargeArray<?>) o;
        return Objects.equals(valueClass, that.valueClass) && Objects.deepEquals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueClass, Arrays.deepHashCode(data));
    }

    private static class LargeArrayIterator<T> implements Iterator<T> {
        private final LargeArray<T> array;
        private LargeArrayIndex currentIndex = LargeArrayIndex.ZERO;

        public LargeArrayIterator(LargeArray<T> array) {
            this.array = array;
        }

        @Override
        public boolean hasNext() {
            return Long.compareUnsigned(currentIndex.toU64(), array.length()) < 0;
        }

        @Override
        public T next() {
            T value = array.get(currentIndex);
            currentIndex = currentIndex.add(1);
            return value;
        }
    }
}
