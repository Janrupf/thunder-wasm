package net.janrupf.thunderwasm.assembler;

import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.runtime.ExternReference;
import net.janrupf.thunderwasm.runtime.FunctionReference;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;

import java.util.HashMap;
import java.util.Map;

public final class WasmTypeConverter {
    private WasmTypeConverter() {
        throw new AssertionError("No instances of WasmTypeConverter are allowed");
    }

    private static final Map<ValueType, JavaType> LOOKUP;
    private static final Map<JavaType, ValueType> REVERSE_LOOKUP;

    static {
        LOOKUP = new HashMap<>();
        REVERSE_LOOKUP = new HashMap<>();

        // Primitives
        LOOKUP.put(NumberType.I32, PrimitiveType.INT);
        REVERSE_LOOKUP.put(PrimitiveType.INT, NumberType.I32);
        LOOKUP.put(NumberType.I64, PrimitiveType.LONG);
        REVERSE_LOOKUP.put(PrimitiveType.LONG, NumberType.I64);
        LOOKUP.put(NumberType.F32, PrimitiveType.FLOAT);
        REVERSE_LOOKUP.put(PrimitiveType.FLOAT, NumberType.F32);
        LOOKUP.put(NumberType.F64, PrimitiveType.DOUBLE);
        REVERSE_LOOKUP.put(PrimitiveType.DOUBLE, NumberType.F64);

        // Objects
        LOOKUP.put(ReferenceType.EXTERNREF, ObjectType.of(ExternReference.class));
        REVERSE_LOOKUP.put(ObjectType.of(ExternReference.class), ReferenceType.EXTERNREF);
        LOOKUP.put(ReferenceType.FUNCREF, ObjectType.of(FunctionReference.class));
        REVERSE_LOOKUP.put(ObjectType.of(FunctionReference.class), ReferenceType.FUNCREF);
    }

    /**
     * Converts a {@link ValueType} to a {@link JavaType}.
     *
     * @param type the type to convert
     * @return the converted type
     * @throws WasmAssemblerException if the type is not supported
     */
    public static JavaType toJavaType(ValueType type) throws WasmAssemblerException {
        JavaType result = LOOKUP.get(type);

        if (result == null) {
            throw new WasmAssemblerException("Unsupported type: " + type);
        }

        return result;
    }

    /**
     * Converts a {@link JavaType} to a {@link ValueType}.
     *
     * @param javaType the Java type to convert
     * @return the converted type
     * @throws WasmAssemblerException if the Java type is not supported
     */
    public static ValueType fromJavaType(JavaType javaType) throws WasmAssemblerException {
        ValueType result = REVERSE_LOOKUP.get(javaType);

        if (result == null) {
            throw new WasmAssemblerException("Unsupported Java type: " + javaType);
        }

        return result;
    }

    /**
     * Converts an array of {@link ValueType}s to an array of {@link JavaType}s.
     *
     * @param types the types to convert
     * @return the converted types
     * @throws WasmAssemblerException if any of the types is not supported
     */
    public static JavaType[] toJavaTypes(ValueType[] types) throws WasmAssemblerException {
        JavaType[] result = new JavaType[types.length];

        for (int i = 0; i < types.length; i++) {
            result[i] = toJavaType(types[i]);
        }

        return result;
    }

    /**
     * Converts an array of {@link JavaType}s to an array of {@link ValueType}s.
     *
     * @param types the Java types to convert
     * @return the converted types
     * @throws WasmAssemblerException if any of the Java types is not supported
     */
    public  static ValueType[] fromJavaTypes(JavaType[] types) throws WasmAssemblerException {
        ValueType[] result = new ValueType[types.length];

        for (int i = 0; i < types.length; i++) {
            result[i] = fromJavaType(types[i]);
        }

        return result;
    }
}
