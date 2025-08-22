package net.janrupf.thunderwasm.runtime.linker.bind;

import net.janrupf.thunderwasm.ThunderWasmException;

/**
 * Exception thrown when a reflective operation fails during runtime linking.
 */
public class ReflectiveRuntimeLinkerException extends ThunderWasmException {
    public ReflectiveRuntimeLinkerException(String message) {
        super(message);
    }

    public ReflectiveRuntimeLinkerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Executes a reflective operation and wraps any thrown exceptions in a ReflectiveRuntimeLinkerException.
     *
     * @param operation the reflective operation to execute
     * @param <T>       the return type of the operation
     * @return the result of the operation
     * @throws ReflectiveRuntimeLinkerException if the operation throws any exception
     */
    public static <T> T catching(ThrowingReflectiveOperation<T> operation) throws ReflectiveRuntimeLinkerException {
        try {
            return operation.execute();
        } catch (Throwable t) {
            throw new ReflectiveRuntimeLinkerException("Reflective operation failed", t);
        }
    }

    /**
     * A functional interface representing a reflective operation that can throw.
     *
     * @param <T> the return type of the operation
     */
    @FunctionalInterface
    public interface ThrowingReflectiveOperation<T> {
        T execute() throws Throwable;
    }
}
