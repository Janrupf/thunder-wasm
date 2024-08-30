package net.janrupf.thunderwasm.lookup;

import net.janrupf.thunderwasm.imports.Import;
import net.janrupf.thunderwasm.imports.ImportDescription;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;

/**
 * Represents an element that was found.
 *
 * @param <E> the type of the element
 */
public final class FoundElement<E, D extends ImportDescription> {
    private final E element;
    private final Import<D> im;
    private final LargeArrayIndex index;

    private FoundElement(
            E element,
            Import<D> im,
            LargeArrayIndex index
    ) {
        this.element = element;
        this.im = im;
        this.index = index;
    }

    /**
     * Retrieves the element that was found.
     *
     * @return the element, or {@code null} if the element is an import
     */
    public E getElement() {
        return element;
    }

    /**
     * Retrieves the import.
     *
     * @return the import, or {@code null} if the element is not an import
     */
    public Import<D> getImport() {
        return im;
    }

    /**
     * Retrieves the index of the element.
     *
     * @return the index
     */
    public LargeArrayIndex getIndex() {
        return index;
    }

    /**
     * Determines whether the element is an import.
     *
     * @return whether the element is an import
     */
    public boolean isImport() {
        return im != null;
    }

    /**
     * Creates a new found element which is an import.
     *
     * @param im    the import
     * @param index the index
     * @param <E>   the type of the element
     * @param <D>   the type of the import description
     * @return the found element
     */
    public static <E, D extends ImportDescription> FoundElement<E, D> ofImport(
            Import<D> im,
            LargeArrayIndex index
    ) {
        return new FoundElement<>(null, im, index);
    }

    /**
     * Creates a new found element which is not an import.
     *
     * @param element the element
     * @param index   the index
     * @param <E>     the type of the element
     * @param <D>     the type of the import description
     * @return the found element
     */
    public static <E, D extends ImportDescription> FoundElement<E, D> ofInternal(
            E element,
            LargeArrayIndex index
    ) {
        return new FoundElement<>(element, null, index);
    }
}
