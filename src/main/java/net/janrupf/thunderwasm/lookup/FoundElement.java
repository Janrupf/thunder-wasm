package net.janrupf.thunderwasm.lookup;

import net.janrupf.thunderwasm.imports.ImportDescription;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;

/**
 * Represents an element that was found.
 *
 * @param <E> the type of the element
 */
public final class FoundElement<E, D extends ImportDescription> {
    private final E element;
    private final D importDescription;
    private final LargeArrayIndex index;

    private FoundElement(
            E element,
            D importDescription,
            LargeArrayIndex index
    ) {
        this.element = element;
        this.importDescription = importDescription;
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
     * Retrieves the import description of the element.
     *
     * @return the import description, or {@code null} if the element is not an import
     */
    public D getImportDescription() {
        return importDescription;
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
        return importDescription != null;
    }

    /**
     * Creates a new found element which is an import.
     *
     * @param importDescription the import description
     * @param index the index
     * @return the found element
     * @param <E> the type of the element
     * @param <D> the type of the import description
     */
    public static <E, D extends ImportDescription> FoundElement<E, D> ofImport(
            D importDescription,
            LargeArrayIndex index
    ) {
        return new FoundElement<>(null, importDescription, index);
    }

    /**
     * Creates a new found element which is not an import.
     *
     * @param element the element
     * @param index the index
     * @return the found element
     * @param <E> the type of the element
     * @param <D> the type of the import description
     */
    public static <E, D extends ImportDescription> FoundElement<E, D> ofInternal(
            E element,
            LargeArrayIndex index
    ) {
        return new FoundElement<>(element, null, index);
    }
}
