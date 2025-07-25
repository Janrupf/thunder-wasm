package net.janrupf.thunderwasm.test.wast;

import org.junit.jupiter.api.Assertions;

import java.lang.invoke.WrongMethodTypeException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Utility class for validating test results and creating meaningful assertion error messages.
 * Provides methods for comparing expected vs actual values with proper handling of
 * floating-point special values and multi-value returns.
 */
public class WastExceptionMatcher {
    private static final HashMap<String, List<Class<? extends Throwable>>> KNOWN_TRAPS;

    static {
        // We mostly rely on Java to actually generate exceptions for illegal operations,
        // however, the WASM testsuite uses hardcoded error messages. Map the known messages
        // to the exception types we expect.
        KNOWN_TRAPS = new HashMap<>();
        KNOWN_TRAPS.put("out of bounds memory access", Collections.singletonList(IndexOutOfBoundsException.class));
        KNOWN_TRAPS.put("undefined element", Collections.singletonList(IndexOutOfBoundsException.class));
        KNOWN_TRAPS.put("uninitialized element", Collections.singletonList(NullPointerException.class));
        KNOWN_TRAPS.put("integer divide by zero", Collections.singletonList(ArithmeticException.class));
        KNOWN_TRAPS.put("call stack exhausted", Collections.singletonList(StackOverflowError.class));
        KNOWN_TRAPS.put("indirect call type mismatch", Collections.singletonList(WrongMethodTypeException.class));
        KNOWN_TRAPS.put("out of bounds table access", Collections.singletonList(IndexOutOfBoundsException.class));
    }

    public static void checkTrap(Throwable actual, String wastMessage) {
        if (!KNOWN_TRAPS.containsKey(wastMessage)) {
            throw new IllegalArgumentException("Don't know how to map trap message \"" + wastMessage + "\" to type (got " + actual.getClass() + ")");
        }

        List<Class<? extends Throwable>> allowedClasses = KNOWN_TRAPS.get(wastMessage);

        for (Class<?> allowed : allowedClasses) {
            if (allowed.isInstance(actual)) {
                return;
            }
        }

        Assertions.fail("Unexpected exception type for \"" + wastMessage + "\": " + actual.getClass() + ": " + actual.getMessage());
    }
}