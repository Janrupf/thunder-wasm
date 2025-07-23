package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.generator.*;
import net.janrupf.thunderwasm.assembler.generator.defaults.*;

/**
 * Holder for all generators used by the {@link net.janrupf.thunderwasm.assembler.WasmAssembler}.
 */
public final class WasmGenerators {
    private FunctionGenerator functionGenerator;
    private GlobalGenerator globalGenerator;
    private ImportGenerator importGenerator;
    private MemoryGenerator memoryGenerator;
    private TableGenerator tableGenerator;

    public WasmGenerators() {
        this.functionGenerator = new DefaultFunctionGenerator();
        this.globalGenerator = new DefaultGlobalGenerator();
        this.importGenerator = new DefaultImportGenerator();
        this.memoryGenerator = new DefaultMemoryGenerator();
        this.tableGenerator = new DefaultTableGenerator();
    }

    /**
     * Overrides the function generator.
     *
     * @param functionGenerator the new function generator
     * @return this
     */
    public WasmGenerators withFunctionGenerator(FunctionGenerator functionGenerator) {
        this.functionGenerator = functionGenerator;
        return this;
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
     * Overrides the memory generator.
     *
     * @param memoryGenerator the new memory generator
     * @return this
     */
    public WasmGenerators withMemoryGenerator(MemoryGenerator memoryGenerator) {
        this.memoryGenerator = memoryGenerator;
        return this;
    }

    /**
     * Overrides the table generator.
     *
     * @param tableGenerator the new table generator
     * @return this
     */
    public WasmGenerators withTableGenerator(TableGenerator tableGenerator) {
        this.tableGenerator = tableGenerator;
        return this;
    }

    /**
     * Retrieves the function generator.
     *
     * @return the function generator
     */
    public FunctionGenerator getFunctionGenerator() {
        return functionGenerator;
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

    /**
     * Retrieves the memory generator.
     *
     * @return the memory generator
     */
    public MemoryGenerator getMemoryGenerator() {
        return memoryGenerator;
    }

    /**
     * Retrieves the table generator.
     *
     * @return the table generator
     */
    public TableGenerator getTableGenerator() {
        return tableGenerator;
    }
}
