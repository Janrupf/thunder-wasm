package net.janrupf.thunderwasm.assembler.emitter;

/**
 * The type of an invoke instruction.
 * <p>
 * This does not include invokedynamic instructions because they require
 * special care.
 */
public enum InvokeType {
    /**
     * An invokeinterface instruction.
     */
    INTERFACE,

    /**
     * An invokespecial instruction.
     */
    SPECIAL,

    /**
     * An invokestatic instruction.
     */
    STATIC,

    /**
     * An invokevirtual instruction.
     */
    VIRTUAL
}
