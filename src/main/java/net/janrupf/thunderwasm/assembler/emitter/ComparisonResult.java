package net.janrupf.thunderwasm.assembler.emitter;

/**
 * Result of a comparison.
 */
public enum ComparisonResult {
    /**
     * The first value is less than the second value.
     */
    LESS_THAN,

    /**
     * The first value is greater than the second value.
     */
    GREATER_THAN,

    /**
     * The first value is equal to the second value.
     */
    EQUAL,

    /**
     * The first value is less than or equal to the second value.
     */
    LESS_THAN_OR_EQUAL,

    /**
     * The first value is greater than or equal to the second value.
     */
    GREATER_THAN_OR_EQUAL,

    /**
     * The first value is not equal to the second value.
     */
    NOT_EQUAL,
}
