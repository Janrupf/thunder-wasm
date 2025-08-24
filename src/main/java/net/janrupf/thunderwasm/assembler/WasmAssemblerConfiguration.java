package net.janrupf.thunderwasm.assembler;

/**
 * Configuration data which influences how the assembler generates code.
 */
public final class WasmAssemblerConfiguration {
    public static final WasmAssemblerConfiguration DEFAULT = builder().build();

    private final boolean enableContinuations;
    private final boolean enableStrictNumerics;
    private final boolean atomicBoundsChecks;

    private WasmAssemblerConfiguration(
            boolean enableContinuations,
            boolean enableStrictNumerics,
            boolean atomicBoundsChecks
    ) {
        this.enableContinuations = enableContinuations;
        this.enableStrictNumerics = enableStrictNumerics;
        this.atomicBoundsChecks = atomicBoundsChecks;
    }

    /**
     * Determines whether continuations are enabled.
     *
     * @return true if continuations are enabled, false otherwise
     */
    public boolean continuationsEnabled() {
        return enableContinuations;
    }

    /**
     * Determines whether strict numeric operations are enabled.
     * <p>
     * Strict numeric operations can be slow on the JVM, but they
     * ensure that numeric operations behave exactly like in a WebAssembly
     * environment. For performance reasons this can be turned off at the
     * expense of no longer being a compliant WebAssembly implementation.
     * <p>
     * This mostly affects edge cases like dealing with very large/small numbers,
     * division by zero and NaN values.
     *
     * @return true if strict numeric operations are enabled, false otherwise
     */
    public boolean strictNumericsEnabled() {
        return enableStrictNumerics;
    }

    /**
     * Determines whether build operations should perform bounds checks before
     * performing the operation and not while performing the operation.
     * <p>
     * This is required for compliance with the WebAssembly specification, but
     * a well-behaved program should never hit a bounds check failure, so for
     * performance reasons this can be turned off.
     * <p>
     * Bounds checks are still performed in terms of sandboxing even if this is
     * disabled (in other words, this does not have any sandbox safety implications).
     * However, a `memory.copy` operation that would go out of bounds might
     * partially succeed if this is disabled.
     *
     * @return true if atomic bounds checks are enabled, false otherwise
     */
    public boolean atomicBoundsChecksEnabled() {
        return atomicBoundsChecks;
    }

    /**
     * Create a new configuration builder.
     *
     * @return the created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean enableContinuations;
        private boolean enableStrictNumerics;
        private boolean atomicBoundsChecks;

        private Builder() {
            this.enableContinuations = false;
            this.enableStrictNumerics = true;
            this.atomicBoundsChecks = true;
        }

        /**
         * Enable generating function continuations.
         *
         * @return this
         */
        public Builder enableContinuations() {
            return enableContinuations(true);
        }

        /**
         * Set whether function continuations are enabled.
         *
         * @param enable whether continuations are enabled
         * @return this
         */
        public Builder enableContinuations(boolean enable) {
            this.enableContinuations = enable;
            return this;
        }

        /**
         * Set whether strict numeric operations are enabled.
         *
         * @param enable whether strict numeric operations are enabled
         * @return this
         */
        public Builder enableStrictNumerics(boolean enable) {
            this.enableStrictNumerics = enable;
            return this;
        }

        /**
         * Set whether bulk operations should bound-check before performing
         * the operation.
         *
         * @param enable whether atomic bounds checks are enabled
         * @return this
         */
        public Builder enableAtomicBoundsChecks(boolean enable) {
            this.atomicBoundsChecks = enable;
            return this;
        }

        /**
         * Finish this builder and build the configuration.
         *
         * @return the built configuration
         */
        public WasmAssemblerConfiguration build() {
            return new WasmAssemblerConfiguration(
                    this.enableContinuations,
                    this.enableStrictNumerics,
                    this.atomicBoundsChecks
            );
        }
    }
}
