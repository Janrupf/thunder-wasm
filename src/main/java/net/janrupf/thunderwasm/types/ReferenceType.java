package net.janrupf.thunderwasm.types;

public class ReferenceType extends ValueType {
    private ReferenceType(String name) {
        super(name);
    }

    /**
     * Represents a reference to a function.
     */
    public static final ReferenceType FUNCREF = new ReferenceType("funcref");

    /**
     * Represents a reference to a external object.
     */
    public static final ReferenceType EXTERNREF = new ReferenceType("externref");
}
