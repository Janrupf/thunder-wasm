package net.janrupf.thunderwasm.assembler.emitter.data;

import java.util.HashMap;
import java.util.Map;

public final class MetadataStorage {
    private final Map<MetadataKey<?>, Object> stored;

    public MetadataStorage() {
        this.stored = new HashMap<>();
    }

    /**
     * Retrieve metadata associated with the given key.
     *
     * @param key the key to retrieve metadata for
     * @param <T> the type of metadata
     * @return the metadata associated with the key, or null if none exists
     */
    public <T> T get(MetadataKey<T> key) {
        return key.getType().cast(stored.get(key));
    }

    /**
     * Add a tag (a key without associated value) to the storage.
     *
     * @param key the key to add as a tag
     */
    public void addTag(MetadataKey<Void> key) {
        put(key, null);
    }

    /**
     * Store metadata associated with the given key.
     *
     * @param key   the key to store metadata for
     * @param value the metadata to store
     * @param <T>   the type of metadata
     */
    public <T> void put(MetadataKey<T> key, T value) {
        stored.put(key, value);
    }

    /**
     * Check if metadata exists for the given key.
     *
     * @param key the key to check for
     * @return true if metadata exists for the key, false otherwise
     */
    public boolean contains(MetadataKey<?> key) {
        return stored.containsKey(key);
    }

    /**
     * Remove metadata associated with the given key.
     *
     * @param key the key to remove metadata for
     * @param <T> the type of metadata
     * @return the removed metadata, or null if none existed
     */
    public <T> T remove(MetadataKey<T> key) {
        return key.getType().cast(stored.remove(key));
    }
}
