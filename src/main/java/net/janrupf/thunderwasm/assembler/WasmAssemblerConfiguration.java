package net.janrupf.thunderwasm.assembler;

/**
 * Configuration data which influences how the assembler generates code.
 */
public final class WasmAssemblerConfiguration {
    public static final WasmAssemblerConfiguration DEFAULT = builder().build();

    private final boolean enableContinuations;
    private final boolean enableStrictNumerics;

    private WasmAssemblerConfiguration(
            boolean enableContinuations,
            boolean enableStrictNumerics
    ) {
        this.enableContinuations = enableContinuations;
        this.enableStrictNumerics = enableStrictNumerics;
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

        private Builder() {
            this.enableContinuations = false;
            this.enableStrictNumerics = true;
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
         * Set wh
         * @return this
         */
        public Builder enableStrictNumerics(boolean enable) {
            this.enableStrictNumerics = enable;
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
                    this.enableStrictNumerics
            );
        }
    }
}
