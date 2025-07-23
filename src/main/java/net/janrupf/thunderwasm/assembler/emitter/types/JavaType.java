package net.janrupf.thunderwasm.assembler.emitter.types;

public abstract class JavaType {
    /**
     * Convert this type to a JVM descriptor.
     *
     * @return the JVM descriptor
     */
    public abstract String toJvmDescriptor();

    /**
     * Retrieves the number of slots this type occupies on the stack.
     *
     * @return the number of slots
     */
    public abstract int getSlotCount();

    /**
     * Returns the default value of this type.
     *
     * @return the default value
     */
    public Object getDefaultValue() {
        return null;
    }

    @Override
    public String toString() {
        return toJvmDescriptor();
    }

    /**
     * Creates a JavaType instance based on the given class.
     *
     * @param clazz the class to create the JavaType for
     * @return the corresponding JavaType instance
     */
    public static JavaType of(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            if (clazz == boolean.class) {
                return PrimitiveType.BOOLEAN;
            } else if (clazz == byte.class) {
                return PrimitiveType.BYTE;
            } else if (clazz == char.class) {
                return PrimitiveType.CHAR;
            } else if (clazz == short.class) {
                return PrimitiveType.SHORT;
            } else if (clazz == int.class) {
                return PrimitiveType.INT;
            } else if (clazz == long.class) {
                return PrimitiveType.LONG;
            } else if (clazz == float.class) {
                return PrimitiveType.FLOAT;
            } else if (clazz == double.class) {
                return PrimitiveType.DOUBLE;
            } else {
                throw new IllegalArgumentException("Unsupported primitive type: " + clazz);
            }
        } else if (clazz.isArray()) {
            JavaType elementType = of(clazz.getComponentType());
            return new ArrayType(elementType);
        } else {
            return ObjectType.of(clazz);
        }
    }
}
