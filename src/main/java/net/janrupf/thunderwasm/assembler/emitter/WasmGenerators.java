package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.generator.ImportGenerator;
import net.janrupf.thunderwasm.assembler.generator.defaults.DefaultGlobalGenerator;
import net.janrupf.thunderwasm.assembler.generator.GlobalGenerator;
import net.janrupf.thunderwasm.assembler.generator.defaults.DefaultImportGenerator;

/**
 * Holder for all generators used by the {@link net.janrupf.thunderwasm.assembler.WasmAssembler}.
 */
public final class WasmGenerators {
    private GlobalGenerator globalGenerator;
    private ImportGenerator importGenerator;

    public WasmGenerators() {
        this.globalGenerator = new DefaultGlobalGenerator();
        this.importGenerator = new DefaultImportGenerator();
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
     * Overrides the import generator.
     *
     * @param importGenerator the new import generator
     * @return this
     */
    public WasmGenerators withImportGenerator(ImportGenerator importGenerator) {
        this.importGenerator = importGenerator;
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

    /**
     * Retrieves the import generator.
     *
     * @return the import generator
     */
    public ImportGenerator getImportGenerator() {
        return importGenerator;
    }
}
