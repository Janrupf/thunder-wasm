package net.janrupf.thunderwasm.assembler;

/**
 * Configuration data which influences how the assembler generates code.
 */
public final class WasmAssemblerConfiguration {
    /**
     * The default configuration - this is a full WASM spec compliant configuration.
     */
    public static final WasmAssemblerConfiguration DEFAULT = builder().build();

    private final boolean enableContinuations;
    private final boolean enableStrictNumerics;
    private final boolean atomicBoundsChecks;
    private final boolean overflowBoundsChecks;

    private WasmAssemblerConfiguration(
            boolean enableContinuations,
            boolean enableStrictNumerics,
            boolean atomicBoundsChecks,
            boolean overflowBoundsChecks
    ) {
        this.enableContinuations = enableContinuations;
        this.enableStrictNumerics = enableStrictNumerics;
        this.atomicBoundsChecks = atomicBoundsChecks;
        this.overflowBoundsChecks = overflowBoundsChecks;
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
     * Determines whether overflow bounds checks are enabled.
     * <p>
     * Overflow bounds checks catch a Java implementation specific issue where addresses
     * may silently overflow and suddenly become valid again. As integer overflow is
     * a non-trapping operation in Java, this leads to a non-compliant behavior.
     * Since memory load/store operations happen very frequently, this can be
     * disabled for performance reasons. A well-formed program is unlikely to
     * ever hit this issue.
     * <p>
     * Bounds checks are still performed in terms of sandboxing even if this is
     * disabled (in other words, this does not have any sandbox safety implications).
     * However, a `*.load/store` operation that would go out of bounds might
     * succeed if this is disabled.
     *
     * @return true if overflow bounds checks are enabled, false otherwise
     */
    public boolean overflowBoundsChecksEnabled() {
        return overflowBoundsChecks;
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
        private boolean overflowBoundsChecks;

        private Builder() {
            this.enableContinuations = false;
            this.enableStrictNumerics = true;
            this.atomicBoundsChecks = true;
            this.overflowBoundsChecks = true;
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
         * Set whether address overflow checks are enabled.
         *
         * @param enable whether overflow bounds checks are enabled
         * @return this
         */
        public Builder enableOverflowBoundsChecks(boolean enable) {
            this.overflowBoundsChecks = enable;
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
                    this.atomicBoundsChecks,
                    this.overflowBoundsChecks
            );
        }
    }
}
