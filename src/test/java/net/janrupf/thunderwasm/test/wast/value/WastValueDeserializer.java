package net.janrupf.thunderwasm.test.wast.value;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Custom Jackson deserializer for WastValue types.
 * Handles the polymorphic deserialization based on the "type" field in the JSON.
 */
public class WastValueDeserializer extends JsonDeserializer<WastValue> {
    
    @Override
    public WastValue deserialize(JsonParser p, DeserializationContext ctxt) 
            throws IOException {
        
        JsonNode node = p.getCodec().readTree(p);
        
        String type = node.get("type").asText();
        
        switch (type) {
            case "i32":
                JsonNode i32ValueNode = node.get("value");
                if (i32ValueNode == null) {
                    return new WastIntValue(0, true);
                }

                return new WastIntValue(parseIntValue(i32ValueNode.asText()));
            case "i64":
                JsonNode i64ValueNode = node.get("value");
                if (i64ValueNode == null) {
                    return new WastIntValue(0, true);
                }

                return new WastLongValue(parseLongValue(i64ValueNode.asText()));
            case "f32":
                JsonNode f32ValueNode = node.get("value");
                if (f32ValueNode == null) {
                    return new WastFloatValue(0, true);
                }

                return new WastFloatValue(parseFloatValue(f32ValueNode.asText()));
            case "f64":
                JsonNode f64ValueNode = node.get("value");
                if (f64ValueNode == null) {
                    return new WastDoubleValue(0, true);
                }

                return new WastDoubleValue(parseDoubleValue(f64ValueNode.asText()));
            case "v128":
                if (node.get("value") == null) {
                    return new WastVectorValue(new byte[0], true);
                }

                return new WastVectorValue(parseVectorValue(node));
            case "funcref":
                JsonNode funcrefValue = node.get("value");
                if (funcrefValue == null) {
                    return new WastFuncrefValue(null, true);
                } else if (funcrefValue.asText().equals("null")) {
                    return new WastFuncrefValue(null);
                } else {
                    return new WastFuncrefValue(Integer.parseUnsignedInt(funcrefValue.asText()));
                }
            case "externref":
                JsonNode externrefValue = node.get("value");
                if (externrefValue == null) {
                    return new WastExternrefValue(null, true);
                } else if (externrefValue.asText().equals("null")) {
                    return new WastExternrefValue(null);
                } else {
                    return new WastExternrefValue(Integer.parseUnsignedInt(externrefValue.asText()));
                }
            default:
                throw new IllegalArgumentException("Unknown WASM value type: " + type);
        }
    }
    
    private static int parseIntValue(String valueStr) {
        if (valueStr.startsWith("0x") || valueStr.startsWith("0X")) {
            return (int) Long.parseLong(valueStr.substring(2), 16);
        } else {
            // Handle unsigned 32-bit integers by parsing as long first
            long value = Long.parseLong(valueStr);
            return (int) value;
        }
    }
    
    private static long parseLongValue(String valueStr) {
        if (valueStr.startsWith("0x") || valueStr.startsWith("0X")) {
            return Long.parseUnsignedLong(valueStr.substring(2), 16);
        } else {
            // Handle unsigned 64-bit integers
            return Long.parseUnsignedLong(valueStr);
        }
    }
    
    private static float parseFloatValue(String valueStr) {
        switch (valueStr.toLowerCase()) {
            case "nan": 
            case "nan:canonical":
            case "nan:arithmetic":
                return Float.NaN;
            case "+inf": 
            case "inf":
                return Float.POSITIVE_INFINITY;
            case "-inf": 
                return Float.NEGATIVE_INFINITY;
        }

        int bitsRepresentation = Integer.parseUnsignedInt(valueStr);
        return Float.intBitsToFloat(bitsRepresentation);
    }
    
    private static double parseDoubleValue(String valueStr) {
        switch (valueStr.toLowerCase()) {
            case "nan": 
            case "nan:canonical":
            case "nan:arithmetic":
                return Double.NaN;
            case "+inf": 
            case "inf":
                return Double.POSITIVE_INFINITY;
            case "-inf": 
                return Double.NEGATIVE_INFINITY;
        }
        long bitsRepresentation = Long.parseUnsignedLong(valueStr);
        return Double.longBitsToDouble(bitsRepresentation);
    }
    
    private static byte[] parseVectorValue(JsonNode node) {
        JsonNode valueArray = node.get("value");
        JsonNode laneTypeNode = node.get("lane_type");
        
        if (valueArray == null || !valueArray.isArray()) {
            // If no value array, return default zero vector
            return new byte[16];
        }
        
        if (laneTypeNode == null) {
            // If no lane_type specified, assume i8 and try to parse
            return parseI8Vector(valueArray);
        }
        
        String laneType = laneTypeNode.asText();
        
        // Convert array of lane values to byte array
        byte[] bytes = new byte[16];
        
        switch (laneType) {
            case "i8":
                return parseI8Vector(valueArray);
                
            case "i16":
                if (valueArray.size() != 8) {
                    throw new IllegalArgumentException("v128 i16 must have 8 values, got " + valueArray.size());
                }
                for (int i = 0; i < 8; i++) {
                    int value = Integer.parseInt(valueArray.get(i).asText());
                    bytes[i * 2] = (byte) (value & 0xFF);
                    bytes[i * 2 + 1] = (byte) ((value >> 8) & 0xFF);
                }
                break;
                
            case "i32":
                if (valueArray.size() != 4) {
                    throw new IllegalArgumentException("v128 i32 must have 4 values, got " + valueArray.size());
                }
                for (int i = 0; i < 4; i++) {
                    long value = Long.parseLong(valueArray.get(i).asText());
                    bytes[i * 4] = (byte) (value & 0xFF);
                    bytes[i * 4 + 1] = (byte) ((value >> 8) & 0xFF);
                    bytes[i * 4 + 2] = (byte) ((value >> 16) & 0xFF);
                    bytes[i * 4 + 3] = (byte) ((value >> 24) & 0xFF);
                }
                break;
                
            case "f32":
                if (valueArray.size() != 4) {
                    throw new IllegalArgumentException("v128 f32 must have 4 values, got " + valueArray.size());
                }
                for (int i = 0; i < 4; i++) {
                    // Handle special float values like NaN
                    float floatValue = parseFloatValue(valueArray.get(i).asText());
                    int value = Float.floatToRawIntBits(floatValue);
                    bytes[i * 4] = (byte) (value & 0xFF);
                    bytes[i * 4 + 1] = (byte) ((value >> 8) & 0xFF);
                    bytes[i * 4 + 2] = (byte) ((value >> 16) & 0xFF);
                    bytes[i * 4 + 3] = (byte) ((value >> 24) & 0xFF);
                }
                break;
                
            case "i64":
                if (valueArray.size() != 2) {
                    throw new IllegalArgumentException("v128 i64 must have 2 values, got " + valueArray.size());
                }
                for (int i = 0; i < 2; i++) {
                    long value = Long.parseUnsignedLong(valueArray.get(i).asText());
                    for (int j = 0; j < 8; j++) {
                        bytes[i * 8 + j] = (byte) ((value >> (j * 8)) & 0xFF);
                    }
                }
                break;
                
            case "f64":
                if (valueArray.size() != 2) {
                    throw new IllegalArgumentException("v128 f64 must have 2 values, got " + valueArray.size());
                }
                for (int i = 0; i < 2; i++) {
                    // Handle special float values like NaN
                    double doubleValue = parseDoubleValue(valueArray.get(i).asText());
                    long value = Double.doubleToRawLongBits(doubleValue);
                    for (int j = 0; j < 8; j++) {
                        bytes[i * 8 + j] = (byte) ((value >> (j * 8)) & 0xFF);
                    }
                }
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported v128 lane type: " + laneType);
        }
        
        return bytes;
    }
    
    private static byte[] parseI8Vector(JsonNode valueArray) {
        if (valueArray.size() != 16) {
            throw new IllegalArgumentException("v128 i8 must have 16 values, got " + valueArray.size());
        }
        byte[] bytes = new byte[16];
        for (int i = 0; i < 16; i++) {
            bytes[i] = (byte) Integer.parseInt(valueArray.get(i).asText());
        }
        return bytes;
    }
}