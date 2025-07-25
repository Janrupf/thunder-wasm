package net.janrupf.thunderwasm.runtime.linker.table;

public interface LinkedTable<T> {
    /**
     * Set the element at the given index.
     *
     * @param index   the index
     * @param element the element
     */
    void set(int index, T element);

    /**
     * Retrieve the element at the given index.
     *
     * @param index the index
     * @return the element
     */
    T get(int index);

    /**
     * Retrieve the current size of the table.
     *
     * @return the size
     */
    int size();

    /**
     * Grow the table by the given number of elements.
     *
     * @param initValue the initial value for the new elements
     * @param n         the number of elements to grow by
     * @return the new size of the table or -1 if growing failed
     */
    int grow(T initValue, int n);

    /**
     * Fill the table with the given value.
     *
     * @param i     the start index
     * @param value the value to fill with
     * @param n     the number of elements to fill
     */
    void fill(int i, T value, int n);

    /**
     * Copy elements from another table.
     *
     * @param d      the start index of the destination table
     * @param s      the start index of the source table
     * @param n      the number of elements to copy
     * @param source the source table
     */
    default void copy(int d, int s, int n, LinkedTable<T> source) {
        if (s + n > source.size() || d + n > size()) {
            throw new IndexOutOfBoundsException();
        }

        if (d <= s) {
            for (int i = 0; i < n; i++) {
                T val = source.get(s + i);
                set(d + i, val);
            }
        } else {
            for (int i = 0; i < n; i++) {
                T val = source.get(s + n - 1);
                set(d + n - 1, val);
            }
        }
    }

    /**
     * Copy elements from an initial array.
     *
     * @param d      the start index of the destination table
     * @param s      the start index of the source array
     * @param n      the number of elements to copy
     * @param source the source array
     */
    default void init(int d, int s, int n, T[] source) {
        for (int i = 0; i < n; i++) {
            set(d + i, source[s + i]);
        }
    }
}
