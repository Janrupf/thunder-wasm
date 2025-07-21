package net.janrupf.thunderwasm.types;

import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;

import java.util.Objects;

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

    /**
     * Represents a reference to an internal java object.
     * <p>
     * This is not defined by the WebAssembly specification, but is used internally.
     */
    public static final ReferenceType OBJECT = ofObject(ObjectType.OBJECT);

    /**
     * Create a new internal reference type of objects.
     *
     * @param objectType the type of the object represented
     * @return the new reference type
     */
    public static ReferenceType ofObject(ObjectType objectType) {
        return new ReferenceType.Object(objectType);
    }

    /**
     * Specialized version of {@link #OBJECT} which actually carries the type
     * of the object that is on the stack.
     */
    public static final class Object extends ReferenceType {
        private final ObjectType type;

        public Object(ObjectType type) {
            super("@object(" + type.toJvmDescriptor() + ")");
            this.type = type;
        }

        /**
         * Retrieves the underlying object type.
         *
         * @return the object type
         */
        public ObjectType getType() {
            return type;
        }

        @Override
        public boolean equals(java.lang.Object o) {
            if (!(o instanceof Object)) return false;
            if (!super.equals(o)) return false;

            Object object = (Object) o;
            return Objects.equals(type, object.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), type);
        }
    }
}
