package net.janrupf.thunderwasm.util;

public final class ObjectUtil {
    private ObjectUtil() {
    }

    /**
     * Force-cast an object to a specific type.
     *
     * @param other the object to cast
     * @param <T>   the type to cast to
     * @return the casted object
     */
    @SuppressWarnings("unchecked")
    public static <T> T forceCast(Object other) {
        return (T) other;
    }
}
