package net.janrupf.thunderwasm.assembler.emitter;

/**
 * The result to generate when detecting a NaN.
 */
public enum NaNTargetResult {
    /**
     * Generate a 1, or 0 if no NaN was detected.
     */
    ONE,

    /**
     * Generate a 0, or 1 if no NaN was detected.
     */
    ZERO,

    /**
     * Generate a -1, or 0 if no NaN was detected.
     */
    MINUS_ONE
}
