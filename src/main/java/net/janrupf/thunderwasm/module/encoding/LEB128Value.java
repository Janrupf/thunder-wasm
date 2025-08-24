package net.janrupf.thunderwasm.module.encoding;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public final class LEB128Value {
    /**
     * Reads a signed 16-bit integer from the input stream using LEB128 encoding.
     *
     * @param stream the input stream to read from
     * @return the decoded signed 16-bit integer
     * @throws IOException if an I/O error occurs or the encoding is invalid
     */
    public static short readS16(InputStream stream) throws IOException {
        return (short) read(stream, 16, true);
    }

    /**
     * Reads an unsigned 16-bit integer from the input stream using LEB128 encoding.
     *
     * @param stream the input stream to read from
     * @return the decoded unsigned 16-bit integer as an int
     * @throws IOException if an I/O error occurs or the encoding is invalid
     */
    public static short readU16(InputStream stream) throws IOException {
        return (short) read(stream, 16, false);
    }

    /**
     * Reads a signed 32-bit integer from the input stream using LEB128 encoding.
     *
     * @param stream the input stream to read from
     * @return the decoded signed 32-bit integer
     * @throws IOException if an I/O error occurs or the encoding is invalid
     */
    public static int readS32(InputStream stream) throws IOException {
        return (int) read(stream, 32, true);
    }

    /**
     * Reads an unsigned 32-bit integer from the input stream using LEB128 encoding.
     *
     * @param stream the input stream to read from
     * @return the decoded unsigned 32-bit integer as an int
     * @throws IOException if an I/O error occurs or the encoding is invalid
     */
    public static int readU32(InputStream stream) throws IOException {
        return (int) read(stream, 32, false);
    }

    /**
     * Reads a signed 64-bit integer from the input stream using LEB128 encoding.
     *
     * @param stream the input stream to read from
     * @return the decoded signed 64-bit integer
     * @throws IOException if an I/O error occurs or the encoding is invalid
     */
    public static long readS64(InputStream stream) throws IOException {
        return read(stream, 64, true);
    }

    /**
     * Reads an unsigned 64-bit integer from the input stream using LEB128 encoding.
     *
     * @param stream the input stream to read from
     * @return the decoded unsigned 64-bit integer as a long
     * @throws IOException if an I/O error occurs or the encoding is invalid
     */
    public static long readU64(InputStream stream) throws IOException {
        return read(stream, 64, false);
    }

    private static long read(InputStream stream, int bits, boolean isSigned) throws IOException {
        long result = 0;
        int shift = 0;
        int i = 0;
        byte current;

        final int maxBytes = (bits + 6) / 7;

        do {
            if (i >= maxBytes) {
                throw new IOException("LEB128 Integer representation too long, exceeds " + maxBytes + " bytes for " + bits + "-bit value");
            }

            int b = stream.read();
            if (b == -1) {
                throw new EOFException("Unexpected end of stream while reading LEB128 value");
            }
            current = (byte) b;

            result |= ((long) (current & 0x7F)) << shift;
            shift += 7;
            i++;
        } while ((current & 0x80) != 0);

        if (i == maxBytes) {
            int usedBitsInLastByte = bits - ((maxBytes - 1) * 7);

            if (usedBitsInLastByte < 7) {
                byte payload = (byte) (current & 0x7f);
                byte unusedBitMask = (byte) (-(1 << usedBitsInLastByte) & 0x7f);

                if (isSigned) {
                    boolean signBitSet = (payload & (1 << (usedBitsInLastByte - 1))) != 0;

                    if (signBitSet) {
                        if ((payload & unusedBitMask) != unusedBitMask) {
                            throw new IOException("LEB128 Integer too large");
                        }
                    } else {
                        if ((payload & unusedBitMask) != 0) {
                            throw new IOException("LEB128 Integer too large");
                        }
                    }
                } else {
                    if ((payload & unusedBitMask) != 0) {
                        throw new IOException("LEB128 Integer too large");
                    }
                }
            }
        }

        if (isSigned) {
            if (shift < 64 && (current & 0x40) != 0) {
                result |= -1L << shift;
            }

            long min = -(1L << (bits - 1));
            long max = (1L << (bits - 1)) - 1;
            if (result < min || result > max) {
                throw new IOException("LEB128 Signed " + bits + "-bit integer out of range: " + result);
            }
        } else {
            if (bits < 64) {
                long max = (1L << bits);
                if (result < 0 || (result >= max && max > 0)) {
                    throw new IOException("LEB128 Unsigned " + bits + "-bit integer out of range: " + result);
                }
            }
        }

        return result;
    }
}
