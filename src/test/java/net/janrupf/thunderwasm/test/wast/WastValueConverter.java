package net.janrupf.thunderwasm.test.wast;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.runtime.ExternReference;
import net.janrupf.thunderwasm.runtime.FunctionReference;
import net.janrupf.thunderwasm.runtime.UnresolvedFunctionReference;
import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;
import net.janrupf.thunderwasm.test.wast.value.*;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Utility class for converting between WastValue objects and Java types.
 * Handles conversion from WAST test values to Java objects suitable for WASM function calls,
 * and back for result validation.
 */
public class WastValueConverter {
    private final Map<Integer, WastExternReferenceValue> definedExternReferences;
    private final MethodHandles.Lookup lookup;

    public WastValueConverter() {
        this.definedExternReferences = new HashMap<>();
        this.lookup = MethodHandles.lookup();
    }

    /**
     * Convert a list of WastValues to java objects.
     *
     * @param module the module for which the conversion is performed
     * @param values the values to convert
     * @return a list of java object representing the values
     */
    public List<Object> convertToJavaValues(Object module, List<WastValue> values) {
        List<Object> output = new ArrayList<>(values.size());

        for (WastValue value : values) {
            output.add(convertToJavaValue(module, value));
        }

        return output;
    }

    /**
     * Converts a single WastValue to java object.
     *
     * @param module    the module for which the conversion is performed
     * @param wastValue the WastValue to convert
     * @return the Java object representation
     */
    public Object convertToJavaValue(Object module, WastValue wastValue) {
        if (wastValue == null) {
            return null;
        } else if (wastValue.isValueWildcard()) {
            throw new IllegalStateException("Cannot convert a wildcard value to a java value");
        }

        ValueType type = wastValue.getType();

        if (type instanceof ReferenceType) {
            // References need a bit of special handling
            if (type.equals(ReferenceType.EXTERNREF)) {
                if (wastValue.getValue() == null) {
                    return new ExternReference(null);
                }

                int id = (int) wastValue.getValue();

                WastExternReferenceValue testValue = this.definedExternReferences.computeIfAbsent(id, WastExternReferenceValue::new);
                return new ExternReference(testValue);
            } else if (type.equals(ReferenceType.FUNCREF)) {
                if (wastValue.getValue() == null) {
                    return new FunctionReference(null);
                }

                if (module == null) {
                    throw new IllegalStateException("Can't convert a function reference if the module is unknown");
                }

                LinkedFunction function = obtainFunctionReference(module, (UnresolvedFunctionReference) wastValue.getValue());
                return new FunctionReference(function);
            } else {
                throw new IllegalArgumentException("Unknown reference type " + type);
            }
        } else {
            return wastValue.getValue();
        }
    }

    private LinkedFunction obtainFunctionReference(
            Object module,
            UnresolvedFunctionReference unresolvedFunctionReference
    ) {
        // This is a hack and depends on the implementation details of how the module
        // is generated. However, for the tests this is fine - we are performing an
        // action that would not be required or possible usually.

        String functionName = "$code_" + Integer.toUnsignedString(unresolvedFunctionReference.getFunctionIndex());
        Class<?> moduleClass = module.getClass();

        Method foundMethod = null;

        for (Method m : moduleClass.getDeclaredMethods()) {
            if (m.getName().equals(functionName)) {
                foundMethod = m;
            }
        }

        if (foundMethod == null) {
            throw new IllegalArgumentException(
                    "The module has no function " + unresolvedFunctionReference.getFunctionIndex());
        }

        int moduleArgumentIndex = -1;
        {
            Class<?>[] parameterTypes = foundMethod.getParameterTypes();

            for (int i = 0; i < parameterTypes.length; i++) {
                if (parameterTypes[i].equals(moduleClass)) {
                    moduleArgumentIndex = i;
                }
            }
        }


        // Make the internal code method accessible and then convert into a linked function much like
        // func.ref would do in the base instruction set
        foundMethod.setAccessible(true);

        MethodHandle handle;
        try {
            handle = this.lookup.unreflect(foundMethod);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not unreflect handle even even though setAccessible succeeded", e);
        }

        // Bind the handle to the module
        if (moduleArgumentIndex != -1) {
            handle = MethodHandles.insertArguments(handle, moduleArgumentIndex, module);
        }

        try {
            return LinkedFunction.Simple.inferFromMethodHandle(handle);
        } catch (WasmAssemblerException e) {
            throw new IllegalStateException("Failed to convert internal method to linked function", e);
        }
    }

    /**
     * Compares two values for equality with appropriate handling of floating-point special values.
     *
     * @param expected the expected WastValue
     * @param actual   the actual WastValue
     * @return true if the values are considered equal
     */
    public static boolean areEqual(WastValue expected, WastValue actual) {
        if (expected == null && actual == null) {
            return true;
        }
        if (expected == null || actual == null) {
            return false;
        }

        ValueType expectedType = expected.getType();
        ValueType actualType = actual.getType();

        if (!expectedType.equals(actualType)) {
            return false;
        }

        Object expectedValue = expected.getValue();
        Object actualValue = actual.getValue();

        if (expectedType instanceof NumberType) {
            NumberType numberType = (NumberType) expectedType;
            if (numberType == NumberType.I32 || numberType == NumberType.I64) {
                // Integer comparison - exact match required
                return expectedValue.equals(actualValue);
            } else if (numberType == NumberType.F32) {
                // Float comparison with NaN handling
                float expectedFloat = ((Number) expectedValue).floatValue();
                float actualFloat = ((Number) actualValue).floatValue();
                if (Float.isNaN(expectedFloat) && Float.isNaN(actualFloat)) {
                    return true; // Both NaN
                }
                return Float.compare(expectedFloat, actualFloat) == 0;
            } else if (numberType == NumberType.F64) {
                // Double comparison with NaN handling
                double expectedDouble = ((Number) expectedValue).doubleValue();
                double actualDouble = ((Number) actualValue).doubleValue();
                if (Double.isNaN(expectedDouble) && Double.isNaN(actualDouble)) {
                    return true; // Both NaN
                }
                return Double.compare(expectedDouble, actualDouble) == 0;
            } else {
                return expectedValue.equals(actualValue);
            }
        } else {
            // Non-numeric types - use equals comparison
            return expectedValue.equals(actualValue);
        }
    }

    /**
     * Creates a descriptive string for any Java object.
     */
    private static String describe(Object value) {
        if (value == null) {
            return "null";
        } else {
            String desc = value + "(" + value.getClass().getSimpleName() + ")";
            if (value instanceof Number) {
                Number num = (Number) value;
                if (value instanceof Float) {
                    float f = num.floatValue();
                    if (Float.isNaN(f)) {
                        return "NaN(f32)";
                    } else if (Float.isInfinite(f)) {
                        return f > 0 ? "+∞(f32)" : "-∞(f32)";
                    } else {
                        return f + "(f32)";
                    }
                } else if (value instanceof Double) {
                    double d = num.doubleValue();
                    if (Double.isNaN(d)) {
                        return "NaN(f64)";
                    } else if (Double.isInfinite(d)) {
                        return d > 0 ? "+∞(f64)" : "-∞(f64)";
                    } else {
                        return d + "(f64)";
                    }
                } else {
                    return desc;
                }
            } else if (value instanceof Object[]) {
                Object[] array = (Object[]) value;
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < array.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(describe(array[i]));
                }
                sb.append("]");
                return sb.toString();
            } else {
                return desc;
            }
        }
    }

    private static class WastExternReferenceValue {
        private final int id;

        public WastExternReferenceValue(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof WastExternReferenceValue)) return false;
            WastExternReferenceValue that = (WastExternReferenceValue) o;
            return id == that.id;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(id);
        }

        @Override
        public String toString() {
            return "WastExternReference(" + id + ")";
        }
    }
}