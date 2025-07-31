package net.janrupf.thunderwasm.runtime.state;

/**
 * Helper used by the generated code to move around multiple values.
 */
public final class MultiValue {
    private final int[] intValues;
    private final long[] longValues;
    private final float[] floatValues;
    private final double[] doubleValues;
    private final Object[] objectValues;

    private int intValueCount;
    private int longValueCount;
    private int floatValueCount;
    private int doubleValueCount;
    private int objectValueCount;

    private MultiValue(
            int intValueCapacity,
            int longValueCapacity,
            int floatValueCapacity,
            int doubleValueCapacity,
            int objectValueCapacity
    ) {
        this.intValues = new int[intValueCapacity];
        this.longValues = new long[longValueCapacity];
        this.floatValues = new float[floatValueCapacity];
        this.doubleValues = new double[doubleValueCapacity];
        this.objectValues = new Object[objectValueCapacity];
    }

    public void putInt(int value) {
        this.intValues[intValueCount++] = value;
    }

    public MultiValue withPutInt(int value) {
        this.putInt(value);
        return this;
    }

    public void putLong(long value) {
        this.longValues[longValueCount++] = value;
    }

    public MultiValue withPutLong(long value) {
        this.putLong(value);
        return this;
    }

    public void putFloat(float value) {
        this.floatValues[floatValueCount++] = value;
    }

    public MultiValue withPutFloat(float value) {
        this.putFloat(value);
        return this;
    }

    public void putDouble(double value) {
        this.doubleValues[doubleValueCount++] = value;
    }

    public MultiValue withPutDouble(double value) {
        this.putDouble(value);
        return this;
    }

    public void putObject(Object value) {
        this.objectValues[objectValueCount++] = value;
    }

    public MultiValue withPutObject(Object value) {
        this.putObject(value);
        return this;
    }

    public int popInt() {
        return this.intValues[--intValueCount];
    }

    public long popLong() {
        return this.longValues[--longValueCount];
    }

    public float popFloat() {
        return this.floatValues[--floatValueCount];
    }

    public double popDouble() {
        return this.doubleValues[--doubleValueCount];
    }

    public Object popObject() {
        return this.objectValues[--objectValueCount];
    }

    public void setInt(int index, int value) {
        this.intValues[index] = value;
    }

    public void setLong(int index, long value) {
        this.longValues[index] = value;
    }

    public void setFloat(int index, float value) {
        this.floatValues[index] = value;
    }

    public void setDouble(int index, double value) {
        this.doubleValues[index] = value;
    }

    public void setObject(int index, Object value) {
        this.objectValues[index] = value;
    }

    public int getInt(int index) {
        return this.intValues[index];
    }

    public long getLong(int index) {
        return this.longValues[index];
    }

    public float getFloat(int index) {
        return this.floatValues[index];
    }

    public double getDouble(int index) {
        return this.doubleValues[index];
    }

    public Object getObject(int index) {
        return this.objectValues[index];
    }

    public static MultiValue allocate(
            int intValueCapacity,
            int longValueCapacity,
            int floatValueCapacity,
            int doubleValueCapacity,
            int objectValueCapacity
    ) {
        return new MultiValue(
                intValueCapacity,
                longValueCapacity,
                floatValueCapacity,
                doubleValueCapacity,
                objectValueCapacity
        );
    }

    public static void putInt(int value, MultiValue multiValue) {
        multiValue.putInt(value);
    }

    public static MultiValue staticWithPutInt(int value, MultiValue multi) {
        return multi.withPutInt(value);
    }

    public static void putLong(long value, MultiValue multiValue) {
        multiValue.putLong(value);
    }

    public static MultiValue staticWithPutLong(long value, MultiValue multi) {
        return multi.withPutLong(value);
    }

    public static void putFloat(float value, MultiValue multiValue) {
        multiValue.putFloat(value);
    }

    public static MultiValue staticWithPutFloat(float value, MultiValue multi) {
        return multi.withPutFloat(value);
    }

    public static void putDouble(double value, MultiValue multiValue) {
        multiValue.putDouble(value);
    }

    public static MultiValue staticWithPutDouble(double value, MultiValue multi) {
        return multi.withPutDouble(value);
    }

    public static void putObject(Object value, MultiValue multiValue) {
        multiValue.putObject(value);
    }

    public static MultiValue staticWithPutObject(Object value, MultiValue multi) {
        return multi.withPutObject(value);
    }

    public static void staticSetInt(int value, MultiValue multi, int index) {
        multi.setInt(index, value);
    }

    public static void staticSetLong(long value, MultiValue multi, int index) {
        multi.setLong(index, value);
    }

    public static void staticSetFloat(float value, MultiValue multi, int index) {
        multi.setFloat(index, value);
    }

    public static void staticSetDouble(double value, MultiValue multi, int index) {
        multi.setDouble(index, value);
    }

    public static void staticSetObject(Object value, MultiValue multi, int index) {
        multi.setObject(index, value);
    }
}