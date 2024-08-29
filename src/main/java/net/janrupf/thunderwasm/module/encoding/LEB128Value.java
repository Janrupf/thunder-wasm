package net.janrupf.thunderwasm.module.encoding;

import java.io.IOException;
import java.io.InputStream;

public class LEB128Value {
    private final int shift;
    private final long merged;

    LEB128Value(int shift, long merged) {
        this.shift = shift;
        this.merged = merged;
    }

    /**
     * Reads a LEB128 value from the given stream.
     *
     * @param stream the stream to read from
     * @return the read LEB128 value
     * @throws IOException if an I/O error occurs
     */
    public static LEB128Value readFrom(InputStream stream) throws IOException {
        long result = 0;
        int shift = 0;
        int current;

        do {
            current = stream.read();
            if (current == -1) {
                throw new IOException("Unexpected end of stream while reading LEB128 value");
            }

            result |= ((long) (current & 0x7F)) << shift;
            shift += 7;
        } while ((current & 0x80) != 0);

        return new LEB128Value(shift, result);
    }

    /**
     * Validates that the LEB128 value does not exceed the given amount of bits.
     *
     * @param bits the maximum amount of bits
     * @throws IOException if the value exceeds the given amount of bits
     */
    private void validateBitCount(int bits) throws IOException {
        if (shift - 7 > bits) {
            throw new IOException("LEB128 value exceeds " + bits + " bits");
        }
    }

    /**
     * Returns the LEB128 value as an unsigned 16-bit integer.
     *
     * @return the LEB128 value as an unsigned 16-bit integer
     * @throws IOException if the value exceeds 16 bits
     */
    public short asUnsignedInt16() throws IOException {
        validateBitCount(16);
        return (short) merged;
    }

    /**
     * Returns the LEB128 value as an unsigned 32-bit integer.
     *
     * @return the LEB128 value as an unsigned 32-bit integer
     * @throws IOException if the value exceeds 32 bits
     */
    public int asUnsignedInt32() throws IOException {
        validateBitCount(32);
        return (int) merged;
    }

    /**
     * Returns the LEB128 value as an unsigned 64-bit integer.
     *
     * @return the LEB128 value as an unsigned 64-bit integer
     * @throws IOException if the value exceeds 64 bits
     */
    public long asUnsignedInt64() throws IOException {
        validateBitCount(64);
        return merged;
    }

    /**
     * Returns the LEB128 value as a signed 16-bit integer.
     *
     * @return the LEB128 value as a signed 16-bit integer
     * @throws IOException if the value exceeds 16 bits
     */
    public short asSignedInt16() throws IOException {
        validateBitCount(16);

        // Apply sign extension shift
        short value = (short) merged;
        if (shift < 16 && (value & (1 << (shift - 1))) != 0) {
            value |= (short) (((short) -1) << shift);
        }

        return value;
    }

    /**
     * Returns the LEB128 value as a signed 32-bit integer.
     *
     * @return the LEB128 value as a signed 32-bit integer
     * @throws IOException if the value exceeds 32 bits
     */
    public int asSignedInt32() throws IOException {
        validateBitCount(32);

        // Apply sign extension shift
        int value = (int) this.merged;
        if (shift < 32 && (value & (1 << (shift - 1))) != 0) {
            value |= -1 << shift;
        }

        return value;
    }

    /**
     * Returns the LEB128 value as a signed 64-bit integer.
     *
     * @return the LEB128 value as a signed 64-bit integer
     * @throws IOException if the value exceeds 64 bits
     */
    public long asSignedInt64() throws IOException {
        validateBitCount(64);

        // Apply sign extension shift
        long value = this.merged;
        if (shift < 64 && (value & (1L << (shift - 1))) != 0) {
            value |= -1L << shift;
        }

        return value;
    }
}
