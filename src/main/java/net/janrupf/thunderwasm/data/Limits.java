package net.janrupf.thunderwasm.data;

import java.util.Objects;

/**
 * Describes a range with a lower and optional upper limit.
 */
public final class Limits {
    private final int min;
    private final Integer max;

    public Limits(int min, Integer max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Retrieves the lower limit.
     *
     * @return the lower limit
     */
    public int getMin() {
        return min;
    }

    /**
     * Retrieves the upper limit.
     *
     * @return the upper limit
     */
    public Integer getMax() {
        return max;
    }

    @Override
    public String toString() {
        return "(" + min + ", " + (max == null ? "..." : max) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Limits)) return false;
        Limits limits = (Limits) o;
        return min == limits.min && Objects.equals(max, limits.max);
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max);
    }
}
