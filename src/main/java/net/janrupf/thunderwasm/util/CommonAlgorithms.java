package net.janrupf.thunderwasm.util;

public final class CommonAlgorithms {
    private CommonAlgorithms() {
        throw new AssertionError("This class cannot be instantiated");
    }

    /**
     * Check if an array contains a specific value.
     *
     * @param array the array to check
     * @param value the value to check for
     * @param <T>   the type of the array and the value
     * @return true if the array contains the value, false otherwise
     */
    public static <T> boolean arrayContains(T[] array, T value) {
        for (T t : array) {
            if (t.equals(value)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a byte array contains a specific value.
     *
     * @param array the array to check
     * @param value the value to check for
     * @return true if the array contains the value, false otherwise
     */
    public static boolean byteArrayContains(byte[] array, byte value) {
        for (byte t : array) {
            if (t == value) {
                return true;
            }
        }

        return false;
    }
}
