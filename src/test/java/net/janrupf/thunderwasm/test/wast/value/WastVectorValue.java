package net.janrupf.thunderwasm.test.wast.value;

import net.janrupf.thunderwasm.types.ValueType;
import net.janrupf.thunderwasm.types.VecType;

import java.util.Arrays;

/**
 * WASM test value for vector types (v128 SIMD).
 */
public final class WastVectorValue extends WastValue {
    private final byte[] value;
    
    public WastVectorValue(byte[] value) {
        this(value, false);
    }

    public WastVectorValue(byte[] value, boolean valueWildcard) {
        super(valueWildcard);

        if (!valueWildcard) {
            if (value.length != 16) {
                throw new IllegalArgumentException("v128 value must be exactly 16 bytes, got " + value.length);
            }
        }

        this.value = value.clone();
    }
    
    
    @Override
    public ValueType getType() {
        return VecType.V128;
    }
    
    @Override
    public byte[] getValue() {
        return value.clone();
    }
    
    /**
     * Gets the vector value as a byte array.
     *
     * @return a copy of the 16-byte vector value
     */
    public byte[] asBytes() {
        return value.clone();
    }
    
    /**
     * Gets the vector value as a hexadecimal string.
     *
     * @return hex representation of the vector
     */
    public String asHexString() {
        StringBuilder hex = new StringBuilder();
        for (byte b : value) {
            hex.append(String.format("%02x", b & 0xFF));
        }
        return hex.toString();
    }
    
    @Override
    public String toString() {
        return "v128:0x" + asHexString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WastVectorValue)) return false;
        WastVectorValue other = (WastVectorValue) obj;
        return Arrays.equals(value, other.value);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }
    
    private static byte[] parseVectorValue(String valueStr) {
        // Handle hex string format (e.g., "0x1234567890abcdef1234567890abcdef")
        if (valueStr.startsWith("0x") || valueStr.startsWith("0X")) {
            String hex = valueStr.substring(2);
            if (hex.length() != 32) { // 128 bits = 32 hex chars
                throw new IllegalArgumentException("v128 hex value must be 32 characters (128 bits), got " + hex.length());
            }
            
            byte[] bytes = new byte[16];
            for (int i = 0; i < 16; i++) {
                int index = i * 2;
                int val = Integer.parseInt(hex.substring(index, index + 2), 16);
                bytes[i] = (byte) val;
            }
            return bytes;
        }
        
        // Handle space-separated byte format (e.g., "0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15")
        String[] parts = valueStr.trim().split("\\s+");
        if (parts.length == 16) {
            byte[] bytes = new byte[16];
            for (int i = 0; i < 16; i++) {
                bytes[i] = (byte) Integer.parseInt(parts[i]);
            }
            return bytes;
        }
        
        throw new IllegalArgumentException("Invalid v128 value format: " + valueStr);
    }
}