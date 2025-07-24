package net.janrupf.thunderwasm.assembler.emitter;

public enum Op {
    /**
     * Add 2 integers.
     */
    IADD,

    /**
     * Subtract 2 integers.
     */
    ISUB,

    /**
     * Multiply 2 integers.
     */
    IMUL,

    /**
     * Divide 2 integers.
     */
    IDIV,

    /**
     * Remainder of division of 2 integers.
     */
    IREM,

    /**
     * Shift integer left.
     */
    ISHL,

    /**
     * Shift integer right.
     */
    ISHR,

    /**
     * Shift integer right unsigned.
     */
    IUSHR,

    /**
     * Negate an integer.
     */
    INEG,

    /**
     * Bitwise and of 2 integers.
     */
    IAND,

    /**
     * Bitwise or of 2 integers.
     */
    IOR,

    /**
     * Xor of 2 integers.
     */
    IXOR,

    /**
     * Swap the top 2 stack elements.
     */
    SWAP,

    /**
     * Add 2 longs.
     */
    LADD,

    /**
     * Subtract 2 longs.
     */
    LSUB,

    /**
     * Multiply 2 longs.
     */
    LMUL,

    /**
     * Divide 2 longs.
     */
    LDIV,

    /**
     * Remainder of division of 2 longs.
     */
    LREM,

    /**
     * Bitwise and of 2 longs.
     */
    LAND,

    /**
     * Bitwise or of 2 longs.
     */
    LOR,

    /**
     * Xor of 2 longs.
     */
    LXOR,

    /**
     * Shift long left.
     */
    LSHL,

    /**
     * Shift long right.
     */
    LSHR,

    /**
     * Shift long right unsigned.
     */
    LUSHR,

    /**
     * Compare 2 longs.
     */
    LCMP,

    /**
     * Negate float.
     */
    FNEG,

    /**
     * Add 2 floats.
     */
    FADD,

    /**
     * Subtract 2 floats.
     */
    FSUB,

    /**
     * Multiply 2 floats.
     */
    FMUL,

    /**
     * Divide 2 floats.
     */
    FDIV,

    /**
     * Compare 2 floats, pushing 1 if NaN is involved.
     */
    FCMPG,

    /**
     * Compare 2 floats, pushing -1 if NaN is involved.
     */
    FCMPL,

    /**
     * Negate double.
     */
    DNEG,

    /**
     * Add 2 doubles.
     */
    DADD,

    /**
     * Subtract 2 doubles.
     */
    DSUB,

    /**
     * Multiply 2 doubles.
     */
    DMUL,

    /**
     * Divide 2 doubles.
     */
    DDIV,

    /**
     * Compare 2 doubles, pushing 1 if NaN is involved.
     */
    DCMPG,

    /**
     * Compare 2 doubles, pushing -1 if NaN is involved.
     */
    DCMPL,

    /**
     * Convert integer to long.
     */
    I2L,

    /**
     * Convert integer to float.
     */
    I2F,

    /**
     * Convert integer to double.
     */
    I2D,

    /**
     * Convert integer to byte.
     */
    I2B,

    /**
     * Convert integer to short.
     */
    I2S,

    /**
     * Convert long to integer.
     */
    L2I,

    /**
     * Convert long to float.
     */
    L2F,

    /**
     * Convert long to double.
     */
    L2D,

    /**
     * Convert float to double.
     */
    F2D,

    /**
     * Convert double to float.
     */
    D2F,

    /**
     * Convert float to integer.
     */
    F2I,

    /**
     * Convert float to long.
     */
    F2L,

    /**
     * Convert double to integer.
     */
    D2I,

    /**
     * Convert double to long.
     */
    D2L,

    /**
     * Throw the object on top of the stack.
     */
    THROW,
}
