package net.janrupf.thunderwasm.assembler.emitter.types;

public final class PrimitiveType extends JavaType {
    private final Class<?> type;
    private final String name;
    private final int slotCount;

    private PrimitiveType(Class<?> type, String name, int slotCount) {
        this.type = type;
        this.name = name;
        this.slotCount = slotCount;
    }

    /**
     * Retrieves the actual class of this type.
     *
     * @return the class of this type
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * Retrieves the name of this type.
     *
     * @return the name of this type
     */
    public String getName() {
        return name;
    }

    @Override
    public String toJvmDescriptor() {
        return getName();
    }

    @Override
    public int getSlotCount() {
        return slotCount;
    }

    public static final PrimitiveType BOOLEAN = new PrimitiveType(boolean.class, "Z", 1);
    public static final PrimitiveType BYTE = new PrimitiveType(byte.class, "B", 1);
    public static final PrimitiveType CHAR = new PrimitiveType(char.class, "C", 1);
    public static final PrimitiveType SHORT = new PrimitiveType(short.class, "S", 1);
    public static final PrimitiveType INT = new PrimitiveType(int.class, "I", 1);
    public static final PrimitiveType LONG = new PrimitiveType(long.class, "J", 2);
    public static final PrimitiveType FLOAT = new PrimitiveType(float.class, "F", 1);
    public static final PrimitiveType DOUBLE = new PrimitiveType(double.class, "D", 2);
    public static final PrimitiveType VOID = new PrimitiveType(void.class, "V", 0);
}
