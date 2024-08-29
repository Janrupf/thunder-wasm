package net.janrupf.thunderwasm.runtime;

@SuppressWarnings("unused") // used by generated code
public final class WasmMath {
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
        if (isSpecialOrZero(value)) {
            return value;
        } else if (value >= -0.5f && value < 0.0f) {
            return -0.0f;
        } else if (value > 0.0f && value <= 0.5f) {
            return +0.0f;
        } else {
            float res = (float) Math.round(value);
            return (res % 2.0f != 0.0f) ? res - 1.0f : res;
        }
    }

    public static double doubleNearest(double value) {
        if (isSpecialOrZero(value)) {
            return value;
        } else if (value >= -0.5d && value < 0.0d) {
            return -0.0d;
        } else if (value > 0.0d && value <= 0.5d) {
            return +0.0d;
        } else {
            double res = Math.round(value);
            return (res % 2.0d != 0.0d) ? res - 1.0d : res;
        }
    }

    private static boolean isSpecialOrZero(float value) {
        return Float.isNaN(value) || Float.isInfinite(value) || value == 0.0f;
    }

    private static boolean isSpecialOrZero(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) || value == 0.0f;
    }
}
