package net.janrupf.thunderwasm.assembler.emitter.types;

/**
 * Represents an array type.
 */
public final class ArrayType extends ObjectType {
    private final JavaType elementType;

    public ArrayType(JavaType elementType) {
        super(null, elementType.toJvmDescriptor() + "[]");
        this.elementType = elementType;
    }

    /**
     * Retrieves the element type of this array type.
     *
     * @return the element type of this array type
     */
    public JavaType getElementType() {
        return elementType;
    }

    @Override
    public String toJvmDescriptor() {
        return "[" + elementType.toJvmDescriptor();
    }
}
