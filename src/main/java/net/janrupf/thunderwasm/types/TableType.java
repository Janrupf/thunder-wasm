package net.janrupf.thunderwasm.types;

import net.janrupf.thunderwasm.data.Limits;

public final class TableType {
    private final ReferenceType elementType;
    private final Limits limits;

    public TableType(ReferenceType elementType, Limits limits) {
        this.elementType = elementType;
        this.limits = limits;
    }

    /**
     * Retrieves the type of the elements in the table.
     *
     * @return the element type
     */
    public ReferenceType getElementType() {
        return elementType;
    }

    /**
     * Retrieves the limits of the table.
     *
     * @return the limits
     */
    public Limits getLimits() {
        return limits;
    }
}
