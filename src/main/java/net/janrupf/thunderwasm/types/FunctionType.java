package net.janrupf.thunderwasm.types;

import net.janrupf.thunderwasm.module.encoding.LargeArray;

import java.util.Objects;

/**
 * The type of WASM function (and also instructions).
 */
public final class FunctionType {
    private final LargeArray<ValueType> inputs;
    private final LargeArray<ValueType> outputs;

    /**
     * Constructs a new function type.
     *
     * @param inputs  the input types
     * @param outputs the output types
     */
    public FunctionType(LargeArray<ValueType> inputs, LargeArray<ValueType> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    /**
     * Retrieves the input types of the function type.
     *
     * @return the input types
     */
    public LargeArray<ValueType> getInputs() {
        return inputs;
    }

    /**
     * Retrieves the output types of the function type.
     *
     * @return the output types
     */
    public LargeArray<ValueType> getOutputs() {
        return outputs;
    }

    @Override
    public String toString() {
        return joinTypesToTuple(inputs) + " -> " + joinTypesToTuple(outputs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FunctionType)) return false;
        FunctionType that = (FunctionType) o;
        return Objects.equals(inputs, that.inputs) && Objects.equals(outputs, that.outputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputs, outputs);
    }

    private static <T> String joinTypesToTuple(LargeArray<T> arr) {
        boolean isFirst = true;

        StringBuilder output = new StringBuilder("(");
        for (T valueType : arr) {
            if (!isFirst) {
                output.append(" ");
            } else {
                isFirst = false;
            }

            output.append(valueType.toString());
        }
        output.append(")");
        return output.toString();
    }
}
