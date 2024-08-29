package net.janrupf.thunderwasm.module.section.segment;

import net.janrupf.thunderwasm.instructions.Expr;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.types.ReferenceType;

public final class ElementSegment {
    private final ReferenceType type;
    private final ElementSegmentMode mode;
    private final LargeArray<Expr> init;

    public ElementSegment(ReferenceType type, ElementSegmentMode mode, LargeArray<Expr> init) {
        this.type = type;
        this.mode = mode;
        this.init = init;
    }

    /**
     * Retrieves the reference type of the element segment.
     *
     * @return the reference type
     */
    public ReferenceType getType() {
        return type;
    }

    /**
     * Retrieves the mode of the element segment.
     *
     * @return the mode
     */
    public ElementSegmentMode getMode() {
        return mode;
    }

    /**
     * Retrieves the initialization expressions of the element segment.
     *
     * @return the initialization expressions
     */
    public LargeArray<Expr> getInit() {
        return init;
    }
}
