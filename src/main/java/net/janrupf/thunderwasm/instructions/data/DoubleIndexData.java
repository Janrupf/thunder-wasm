package net.janrupf.thunderwasm.instructions.data;

import net.janrupf.thunderwasm.instructions.WasmInstruction;

/**
 * A data structure that holds two {@link IndexData} instances.
 *
 * @param <F> the type of the first {@link IndexData}
 * @param <S> the type of the second {@link IndexData}
 */
public final class DoubleIndexData<F extends IndexData, S extends IndexData> implements WasmInstruction.Data {
    private final F first;
    private final S second;

    public DoubleIndexData(F first, S second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Returns the first {@link IndexData}.
     *
     * @return the first {@link IndexData}
     */
    public F getFirst() {
        return first;
    }

    /**
     * Returns the second {@link IndexData}.
     *
     * @return the second {@link IndexData}
     */
    public S getSecond() {
        return second;
    }
}
