package net.janrupf.thunderwasm.assembler.generator.defaults;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.runtime.linker.global.*;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ValueType;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper for determining the appropriate field type for a given Java type.
 */
public class DefaultFieldTypeLookup {
    /**
     * Field types to use for global imports.
     */
    public static final DefaultFieldTypeLookup GLOBAL_IMPORT = new DefaultFieldTypeLookup()
            .with(NumberType.I32, LinkedReadOnlyIntGlobal.class, LinkedIntGlobal.class)
            .with(NumberType.I64, LinkedReadOnlyLongGlobal.class, LinkedLongGlobal.class)
            .with(NumberType.F32, LinkedReadOnlyFloatGlobal.class, LinkedFloatGlobal.class)
            .with(NumberType.F64, LinkedReadOnlyDoubleGlobal.class, LinkedDoubleGlobal.class)
            .withDefault(LinkedReadOnlyObjectGlobal.class, LinkedObjectGlobal.class);

    private final Map<JavaType, ReadWriteSelector> mapping;
    private ReadWriteSelector defaultSelector;

    public DefaultFieldTypeLookup() {
        this.mapping = new HashMap<>();
    }

    public DefaultFieldTypeLookup with(JavaType javaType, ObjectType readOnly, ObjectType readWrite, boolean generic) {
        mapping.put(javaType, new ReadWriteSelector(readOnly, readWrite, generic));
        return this;
    }

    public DefaultFieldTypeLookup with(ValueType type, Class<?> readOnly, Class<?> readWrite) {
        try {
            return with(
                    WasmTypeConverter.toJavaType(type),
                    ObjectType.of(readOnly),
                    ObjectType.of(readWrite),
                    readOnly.getTypeParameters().length > 0
            );
        } catch (WasmAssemblerException e) {
            throw new RuntimeException(e);
        }
    }

    public DefaultFieldTypeLookup withDefault(ObjectType readOnly, ObjectType readWrite, boolean generic) {
        defaultSelector = new ReadWriteSelector(readOnly, readWrite, generic);
        return this;
    }

    public DefaultFieldTypeLookup withDefault(Class<?> readOnly, Class<?> readWrite) {
        return withDefault(
                ObjectType.of(readOnly),
                ObjectType.of(readWrite),
                readOnly.getTypeParameters().length > 0
        );
    }

    /**
     * Select the appropriate field type for a given WASM type.
     *
     * @param type       the WASM type
     * @param isReadOnly whether the field should be read-only
     * @return the selected field type
     * @throws WasmAssemblerException if no field type is found
     */
    public Selected select(ValueType type, boolean isReadOnly) throws WasmAssemblerException {
        return select(WasmTypeConverter.toJavaType(type), isReadOnly);
    }

    /**
     * Select the appropriate field type for a given Java type.
     *
     * @param javaType   the Java type
     * @param isReadOnly whether the field should be read-only
     * @return the selected field type
     * @throws WasmAssemblerException if no field type is found
     */
    public Selected select(JavaType javaType, boolean isReadOnly) throws WasmAssemblerException {
        ReadWriteSelector selector = mapping.get(javaType);
        if (selector == null) {
            selector = defaultSelector;
        }

        if (selector == null) {
            throw new WasmAssemblerException("No field type found for " + javaType);
        }

        return new Selected(selector.select(isReadOnly), selector.isGeneric());
    }


    private static class ReadWriteSelector {
        private final ObjectType readOnly;
        private final ObjectType readWrite;
        private final boolean generic;

        public ReadWriteSelector(ObjectType readOnly, ObjectType readWrite, boolean generic) {
            this.readOnly = readOnly;
            this.readWrite = readWrite;
            this.generic = generic;
        }

        /**
         * Selects the appropriate type based on the read-only flag.
         *
         * @param readOnly whether the type should be read-only
         * @return the selected type
         */
        public ObjectType select(boolean readOnly) {
            return readOnly ? this.readOnly : this.readWrite;
        }

        /**
         * Determines whether the selected type is generic.
         *
         * @return whether the selected type is generic
         */
        public boolean isGeneric() {
            return generic;
        }
    }

    /**
     * Result of selecting a field type.
     */
    public static class Selected {
        private final ObjectType type;
        private final boolean isGeneric;

        public Selected(ObjectType type, boolean isGeneric) {
            this.type = type;
            this.isGeneric = isGeneric;
        }

        /**
         * Retrieves the selected type.
         *
         * @return the selected type
         */
        public ObjectType getType() {
            return type;
        }

        /**
         * Determines whether the selected type is generic.
         *
         * @return whether the selected type is generic
         */
        public boolean isGeneric() {
            return isGeneric;
        }
    }
}
