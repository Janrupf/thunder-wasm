package net.janrupf.thunderwasm.assembler;

/**
 * Configuration data which influences how the assembler generates code.
 */
public final class WasmAssemblerConfiguration {
    public static final WasmAssemblerConfiguration DEFAULT = builder().build();

    private final boolean enableContinuations;

    private WasmAssemblerConfiguration(
            boolean enableContinuations
    ) {
        this.enableContinuations = enableContinuations;
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
     * Create a new configuration builder.
     *
     * @return the created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean enableContinuations;

        private Builder() {
            this.enableContinuations = false;
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
         * Finish this builder and build the configuration.
         *
         * @return the built configuration
         */
        public WasmAssemblerConfiguration build() {
            return new WasmAssemblerConfiguration(
                    this.enableContinuations
            );
        }
    }
}
