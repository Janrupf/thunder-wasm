package net.janrupf.thunderwasm.types;

import java.util.Objects;

/**
 * Marker interface for block types.
 */
public interface BlockType {
    /**
     * Represents an empty block type.
     */
    class Empty implements BlockType {
        public static final Empty INSTANCE = new Empty();

        private Empty() {}
    }

    /**
     * Represents a block type with a value type.
     */
    class Value implements BlockType {
        private final ValueType valueType;

        public Value(ValueType valueType) {
            this.valueType = valueType;
        }

        public ValueType getValueType() {
            return valueType;
        }
    }

    /**
     * Represents a block type with a type index.
     */
    class TypeIndex implements BlockType {
        private final int typeIndex;

        public TypeIndex(int typeIndex) {
            this.typeIndex = typeIndex;
        }

        public int getTypeIndex() {
            return typeIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TypeIndex)) return false;
            TypeIndex to = (TypeIndex) o;
            return typeIndex == to.typeIndex;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(typeIndex);
        }
    }
}
