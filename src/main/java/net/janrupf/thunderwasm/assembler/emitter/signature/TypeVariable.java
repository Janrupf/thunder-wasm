package net.janrupf.thunderwasm.assembler.emitter.signature;

/**
 * Represents a type variable in a signature.
 */
public final class TypeVariable implements SignaturePart {
    private final String name;

    public TypeVariable(String name) {
        this.name = name;
    }

    /**
     * Retrieves the name of the type variable.
     *
     * @return the name of the type variable
     */
    public String getName() {
        return name;
    }
}
