package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.generator.DefaultGlobalGenerator;
import net.janrupf.thunderwasm.assembler.generator.GlobalGenerator;

/**
 * Holder for all generators used by the {@link net.janrupf.thunderwasm.assembler.WasmAssembler}.
 */
public final class WasmGenerators {
    private GlobalGenerator globalGenerator;

    public WasmGenerators() {
        this.globalGenerator = new DefaultGlobalGenerator();
    }

    /**
     * Overrides the global generator.
     *
     * @param globalGenerator the new global generator
     * @return this
     */
    public WasmGenerators withGlobalGenerator(GlobalGenerator globalGenerator) {
        this.globalGenerator = globalGenerator;
        return this;
    }

    /**
     * Retrieves the global generator.
     *
     * @return the global generator
     */
    public GlobalGenerator getGlobalGenerator() {
        return globalGenerator;
    }
}
