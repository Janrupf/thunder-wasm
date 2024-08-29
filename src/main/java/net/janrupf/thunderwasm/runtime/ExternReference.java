package net.janrupf.thunderwasm.runtime;

import net.janrupf.thunderwasm.types.ReferenceType;

public final class ExternReference extends ElementReference {
    private final Object referent;

    public ExternReference(Object referent) {
        this.referent = referent;
    }

    /**
     * The referent of the external reference.
     *
     * @return the referent
     */
    public Object getReferent() {
        return referent;
    }

    @Override
    public boolean isNull() {
        return referent == null;
    }

    @Override
    public ReferenceType getType() {
        return ReferenceType.EXTERNREF;
    }

    /**
     * Creates a new (possibly null) external reference.
     *
     * @param referent the referent
     * @return the external reference
     */
    public static ExternReference of(Object referent) {
        return new ExternReference(referent);
    }

    /**
     * Creates a new null external reference.
     *
     * @return the null external reference
     */
    public static ExternReference ofNull() {
        return new ExternReference(null);
    }
}
