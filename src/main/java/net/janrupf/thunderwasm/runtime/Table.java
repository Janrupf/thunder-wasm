package net.janrupf.thunderwasm.runtime;

import net.janrupf.thunderwasm.runtime.linker.table.LinkedTable;

/**
 * WebAssembly table.
 *
 * @param <T> the type of the elements in the table
 */
public final class Table<T> implements LinkedTable<T> {
    private T[] elements;
    private final int limit;

    @SuppressWarnings("unchecked")
    public Table(int min, int limit) {
        if (min < 0 || (limit < 0 && limit != -1)) {
            // The limits are probably unsigned, we do not support this for now
            throw new UnsupportedOperationException("Table limits too large");
        }

        this.elements = (T[]) new Object[min];
        this.limit = limit;
    }

    @Override
    public void set(int index, T element) {
        elements[index] = element;
    }

    @Override
    public T get(int index) {
        return elements[index];
    }

    @Override
    public int size() {
        return elements.length;
    }

    @SuppressWarnings("unchecked")
    @Override
    public int grow(T initValue, int n) {
        int newSize = elements.length + n;
        if (newSize < 0 || (newSize > limit && limit != -1)) {
            // Growing the table failed
            return -1;
        }

        int oldSize = elements.length;

        T[] newElements = (T[]) new Object[newSize];
        System.arraycopy(elements, 0, newElements, 0, elements.length);
        for (int i = elements.length; i < newSize; i++) {
            newElements[i] = initValue;
        }

        elements = newElements;

        return oldSize;
    }

    @Override
    public void fill(int i, T value, int n) {
        for (int j = 0; j < n; j++) {
            elements[i + j] = value;
        }
    }

    @Override
    public void copy(int d, int s, int n, LinkedTable<T> source) {
        if (!(source instanceof Table)) {
            // Use generic implementation
            LinkedTable.super.copy(n, s, d, source);
            return;
        }

        Table<T> otherTable = (Table<T>) source;

        // Perform fast copy
        System.arraycopy(
                otherTable.elements,
                s,
                elements,
                d,
                n
        );
    }

    @Override
    public void init(int d, int s, int n, T[] source) {
        System.arraycopy(source, s, elements, d, n);
    }
}
