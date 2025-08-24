package net.janrupf.thunderwasm.runtime;

@SuppressWarnings("unused") // used by generated code
public final class WasmMath {
    private static final double U32_UPPER_BOUND = 4294967296.0;
    private static final double U32_LOWER_BOUND = -1.0;
    private static final double S32_UPPER_BOUND = 2147483648.0;
    private static final double S32_LOWER_BOUND = -2147483649.0;

    private static final double S64_UPPER_BOUND = 9223372036854775808.0;
    private static final double S64_LOWER_BOUND_INCLUSIVE = -9223372036854775808.0;

    private static final double TWO_POW_64 = Math.pow(2, 64);

    private WasmMath() {}

    public static float floatTrunc(float value) {
        if (isSpecialOrZero(value)) {
            return value;
        } else if (value > 0.0f && value < 1.0f) {
            return +0.0f;
        } else if (value < 0.0f && value > -1.0f) {
            return -0.0f;
        } else if (value > 0.0f) {
            return (float) Math.floor(value);
        } else {
            return (float) Math.ceil(value);
        }
    }

    public static double doubleTrunc(double value) {
        if (isSpecialOrZero(value)) {
            return value;
        } else if (value > 0.0d && value < 1.0d) {
            return +0.0d;
        } else if (value < 0.0d && value > -1.0d) {
            return -0.0d;
        } else if (value > 0.0d) {
            return Math.floor(value);
        } else {
            return Math.ceil(value);
        }
    }

    public static float floatNearest(float value) {
        return (float) Math.rint(value);
    }

    public static double doubleNearest(double value) {
        return Math.rint(value);
    }

    private static boolean isSpecialOrZero(float value) {
        return Float.isNaN(value) || Float.isInfinite(value) || value == 0.0f;
    }

    private static boolean isSpecialOrZero(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) || value == 0.0f;
    }

    public static float u32ToF32(int value) {
        return (float) Integer.toUnsignedLong(value);
    }

    public static float u64ToF32(long value) {
        if (value >= 0) {
            return (float) value;
        }

        int leadingZeros = Long.numberOfLeadingZeros(value);
        int exponent = 63 - leadingZeros;

        int shift = exponent - 23;

        if (shift <= 0) {
            return (float) ((value >>> 1) * 2.0 + (value & 1));
        }

        long mantissaBits = value >>> shift;
        long truncatedBits = value & ((1L << shift) - 1);

        long halfwayPoint = 1L << (shift - 1);
        if (truncatedBits > halfwayPoint ||
                (truncatedBits == halfwayPoint && (mantissaBits & 1) == 1)) {
            mantissaBits++;
            if (mantissaBits == (1L << 24)) {
                mantissaBits = 1L << 23;
                exponent++;
            }
        }

        int floatBits = ((exponent + 127) << 23) | ((int)mantissaBits & 0x7FFFFF);
        return Float.intBitsToFloat(floatBits);
    }

    public static double u32ToF64(int value) {
        return (double) (value & 0xFFFFFFFFL);
    }

    public static double u64ToF64(long value) {
        if (value >= 0) {
            return (double) value;
        }

        double high = (double)((value >>> 32) & 0xFFFFFFFFL) * 4294967296.0;
        double low = (double)(value & 0xFFFFFFFFL);
        return high + low;
    }

    public static int strictI32TruncF32S(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new ArithmeticException("Cannot convert NaN or Infinity to integer");
        }

        if (value > S32_LOWER_BOUND && value < S32_UPPER_BOUND) {
            return (int) value;
        } else {
            throw new ArithmeticException("Integer overflow");
        }
    }

    public static int strictI32TruncF32U(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new ArithmeticException("Cannot convert NaN or Infinity to integer");
        }

        if (value > U32_LOWER_BOUND && value < U32_UPPER_BOUND) {
            return (int) (long) value;
        } else {
            throw new ArithmeticException("Integer overflow");
        }
    }

    public static int strictI32TruncF64S(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new ArithmeticException("Cannot convert NaN or Infinity to integer");
        }

        if (value > S32_LOWER_BOUND && value < S32_UPPER_BOUND) {
            return (int) value;
        } else {
            throw new ArithmeticException("Integer overflow");
        }
    }

    public static int strictI32TruncF64U(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new ArithmeticException("Cannot convert NaN or Infinity to integer");
        }

        if (value > U32_LOWER_BOUND && value < U32_UPPER_BOUND) {
            return (int) (long) value;
        } else {
            throw new ArithmeticException("Integer overflow");
        }
    }

    public static long strictI64TruncF32S(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new ArithmeticException("Cannot convert NaN or Infinity to integer");
        }

        if (value >= S64_LOWER_BOUND_INCLUSIVE && value < S64_UPPER_BOUND) {
            return (long) value;
        } else {
            throw new ArithmeticException("Integer overflow");
        }
    }

    public static long strictI64TruncF32U(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new ArithmeticException("Cannot convert NaN or Infinity to integer");
        }

        if (value <= -1.0f || value >= 0x1p64f) {
            throw new ArithmeticException("integer overflow");
        }

        if (value < 1.0f) {
            return 0L;
        } else if (value < 0x1p63f) {
            return (long) value;
        }

        return (long)(value - 0x1p63f) ^ Long.MIN_VALUE;
    }

    public static long strictI64TruncF64S(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new ArithmeticException("Cannot convert NaN or Infinity to integer");
        }

        if (value >= S64_LOWER_BOUND_INCLUSIVE && value < S64_UPPER_BOUND) {
            return (long) value;
        } else {
            throw new ArithmeticException("Integer overflow");
        }
    }

    public static long strictI64TruncF64U(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new ArithmeticException("Cannot convert NaN or Infinity to integer");
        }

        if (value <= -1.0 || value >= TWO_POW_64) {
            throw new ArithmeticException("integer overflow");
        }

        if (value < 0x1p63) {
            return (long) value;
        }

        return (long)(value - 0x1p63) ^ Long.MIN_VALUE;
    }

    public static int strictI32TruncSatF32U(float value) {
        if (Float.isNaN(value)) {
            return 0;
        }

        if (value > U32_LOWER_BOUND && value < U32_UPPER_BOUND) {
            return (int) (long) value;
        } else if (value >= U32_UPPER_BOUND) {
            return -1;
        } else {
            return 0;
        }
    }

    public static int strictI32TruncSatF64U(double value) {
        if (Double.isNaN(value)) {
            return 0;
        }

        if (value > U32_LOWER_BOUND && value < U32_UPPER_BOUND) {
            return (int) (long) value;
        } else if (value >= U32_UPPER_BOUND) {
            return -1;
        } else {
            return 0;
        }
    }

    public static long strictI64TruncSatF32U(float value) {
        if (Float.isNaN(value)) {
            return 0L;
        }

        if (value <= -1.0f) {
            return 0L;
        } else if (value >= 0x1p64f) {
            return -1L;
        } else if (value < 1.0f) {
            return 0L;
        } else if (value < 0x1p63f) {
            return (long) value;
        }

        return (long)(value - 0x1p63f) ^ Long.MIN_VALUE;
    }

    public static long strictI64TruncSatF64U(double value) {
        if (Double.isNaN(value)) {
            return 0L;
        }

        if (value <= -1.0) {
            return 0L;
        } else if (value >= TWO_POW_64) {
            return -1L;
        } else if (value < 0x1p63) {
            return (long) value;
        }

        return (long)(value - 0x1p63) ^ Long.MIN_VALUE;
    }
}
