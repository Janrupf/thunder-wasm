package net.janrupf.thunderwasm.assembler.emitter.data;

import java.util.Arrays;
import java.util.Objects;

/**
 * A key used to store and retrieve metadata in a {@link MetadataStorage}.
 *
 * @param <T> the type of metadata this key is associated with
 */
public abstract class MetadataKey<T> {
    private final Class<T> type;

    private MetadataKey(Class<T> type) {
        this.type = type;
    }

    /**
     * Retrieve the type of metadata this key is associated with.
     *
     * @return the type of metadata
     */
    public final Class<T> getType() {
        return type;
    }

    /**
     * Create a unique metadata key that is only equal to itself.
     *
     * @return the unique metadata key
     */
    public static MetadataKey<Void> unique() {
        return unique(Void.class);
    }

    /**
     * Create a unique metadata key that is only equal to itself.
     *
     * @param type the type of metadata this key is associated with
     * @return the unique metadata key
     */
    public static <T> MetadataKey<T> unique(Class<T> type) {
        return new Unique<>(type);
    }

    /**
     * Create a named metadata key that is equal to other keys with the same name.
     * <p>
     * Note that this can potentially lead to collisions if the same name is used
     * for different purposes or types. Use with caution.
     *
     * @param name the name of the metadata key
     * @return the named metadata key
     */
    public static MetadataKey<Void> named(String name) {
        return named(Void.class, name);
    }

    /**
     * Create a named metadata key that is equal to other keys with the same name.
     * <p>
     * Note that this can potentially lead to collisions if the same name is used
     * for different purposes or types. Use with caution.
     *
     * @param type the type of metadata this key is associated with
     * @param name the name of the metadata key
     * @param <T>  the type of metadata
     * @return the named metadata key
     */
    public static <T> MetadataKey<T> named(Class<T> type, String name) {
        return new Named<>(type, name);
    }

    /**
     * Create a composed metadata key that is equal to other keys with the same components.
     *
     * @param components the components that make up this key
     * @return the composed metadata key
     */
    public static MetadataKey<Void> compose(MetadataKey<?>... components) {
        return compose(Void.class, components);
    }

    /**
     * Create a composed metadata key that is equal to other keys with the same components.
     *
     * @param type       the type of metadata this key is associated with
     * @param components the components that make up this key
     * @param <T>        the type of metadata
     * @return the composed metadata key
     */
    public static <T> MetadataKey<T> compose(Class<T> type, MetadataKey<?>... components) {
        return new Composed<>(type, components);
    }

    private static final class Unique<T> extends MetadataKey<T> {
        private Unique(Class<T> type) {
            super(type);
        }
    }

    private static final class Named<T> extends MetadataKey<T> {
        private final String name;

        private Named(Class<T> type, String name) {
            super(type);
            this.name = name;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Named<?> named = (Named<?>) obj;
            return name.equals(named.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    private static final class Composed<T> extends MetadataKey<T> {
        private final MetadataKey<?>[] components;

        private Composed(Class<T> type, MetadataKey<?>[] components) {
            super(type);
            this.components = components;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Composed)) return false;
            Composed<?> composed = (Composed<?>) o;
            return Objects.deepEquals(components, composed.components);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(components);
        }
    }
}
