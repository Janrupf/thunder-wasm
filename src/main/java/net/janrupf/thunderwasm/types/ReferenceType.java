package net.janrupf.thunderwasm.types;

public final class ReferenceType extends ValueType {
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

    /**
     * Represents a reference to an internal java object.
     * <p>
     * This is not defined by the WebAssembly specification, but is used internally.
     */
    public static final ReferenceType OBJECT = new ReferenceType("@object");
}
