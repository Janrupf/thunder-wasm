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

    static {
        LOOKUP = new HashMap<>();

        // Primitives
        LOOKUP.put(NumberType.I32, PrimitiveType.INT);
        LOOKUP.put(NumberType.I64, PrimitiveType.LONG);
        LOOKUP.put(NumberType.F32, PrimitiveType.FLOAT);
        LOOKUP.put(NumberType.F64, PrimitiveType.DOUBLE);

        // Objects
        LOOKUP.put(ReferenceType.EXTERNREF, ObjectType.of(ExternReference.class));
        LOOKUP.put(ReferenceType.FUNCREF, ObjectType.of(FunctionReference.class));
        LOOKUP.put(ReferenceType.OBJECT, ObjectType.OBJECT);
    }

    /**
     * Converts a {@link ValueType} to a {@link JavaType}.
     *
     * @param type the type to convert
     * @return the converted type
     * @throws WasmAssemblerException if the type is not supported
     */
    public static JavaType toJavaType(ValueType type) throws WasmAssemblerException {
        if (type instanceof ReferenceType.Object) {
            return ((ReferenceType.Object) type).getType();
        }

        JavaType result = LOOKUP.get(type);

        if (result == null) {
            throw new WasmAssemblerException("Unsupported type: " + type);
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
}
