package net.janrupf.thunderwasm.assembler.emitter.signature;

import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a concrete type in a signature.
 */
public final class ConcreteType implements SignaturePart {
    private final ObjectType type;
    private final List<SignaturePart> typeArguments;

    public ConcreteType(ObjectType type, List<SignaturePart> typeArguments) {
        this.type = type;
        this.typeArguments = typeArguments == null ? Collections.emptyList() : typeArguments;
    }

    /**
     * Retrieves the type of the concrete type.
     *
     * @return the type of the concrete type
     */
    public ObjectType getType() {
        return type;
    }

    /**
     * Retrieves the type arguments of the concrete type.
     *
     * @return the type arguments of the concrete type
     */
    public List<SignaturePart> getTypeArguments() {
        return typeArguments;
    }

    /**
     * Creates a concrete type with no type arguments.
     *
     * @param type the type of the concrete type
     * @return the concrete type
     */
    public static ConcreteType of(ObjectType type) {
        return new ConcreteType(type, null);
    }

    /**
     * Creates a new builder for a concrete type.
     *
     * @param type the type of the concrete type
     * @return the builder
     */
    public static Builder builder(ObjectType type) {
        return new Builder(type);
    }

    public static final class Builder {
        private ObjectType type;
        private final List<SignaturePart> typeArguments;

        private Builder(ObjectType type) {
            this.type = type;
            this.typeArguments = new ArrayList<>();
        }

        /**
         * Adds a type argument to the concrete type.
         *
         * @param typeArgument the type argument
         * @return this builder
         */
        public Builder withTypeArgument(SignaturePart typeArgument) {
            typeArguments.add(typeArgument);
            return this;
        }

        /**
         * Builds the concrete type.
         *
         * @return the concrete type
         */
        public ConcreteType build() {
            return new ConcreteType(type, typeArguments);
        }
    }
}
