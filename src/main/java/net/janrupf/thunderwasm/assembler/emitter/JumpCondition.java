package net.janrupf.thunderwasm.assembler.emitter;

/**
 * Condition to jump under.
 */
public enum JumpCondition {
    /**
     * Jump if integers are equal.
     */
    INT_EQUAL,

    /**
     * Jump if integers are not equal.
     */
    INT_NOT_EQUAL,

    /**
     * Jump if integer is less than some other integer.
     */
    INT_LESS_THAN,

    /**
     * Jump if integer is greater than some other integer.
     */
    INT_GREATER_THAN,

    /**
     * Jump if integer is less than or equal to some other integer.
     */
    INT_LESS_THAN_OR_EQUAL,

    /**
     * Jump if integer is greater than or equal to some other integer.
     */
    INT_GREATER_THAN_OR_EQUAL,

    /**
     * Jump if integer is equal to zero.
     */
    INT_EQUAL_ZERO,

    /**
     * Jump if integer is not equal to zero.
     */
    INT_NOT_EQUAL_ZERO,

    /**
     * Jump if integer is less than zero.
     */
    INT_LESS_THAN_ZERO,

    /**
     * Jump if integer is greater than.
     */
    INT_GREATER_THAN_ZERO,

    /**
     * Jump if integer is less than or equal to zero.
     */
    INT_LESS_THAN_OR_EQUAL_ZERO,

    /**
     * Jump if integer is greater than or equal to zero.
     */
    INT_GREATER_THAN_OR_EQUAL_ZERO,

    /**
     * Jump if reference is equal.
     */
    REFERENCE_IS_EQUAL,

    /**
     * Jump if reference is not equal.
     */
    REFERENCE_IS_NOT_EQUAL,

    /**
     * Jump if reference is null.
     */
    IS_NULL,

    /**
     * Jump if reference is not null.
     */
    IS_NOT_NULL,

    /**
     * Jump always.
     */
    ALWAYS,
}
