package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.WasmAssemblerConfiguration;
import net.janrupf.thunderwasm.assembler.emitter.data.MetadataStorage;
import net.janrupf.thunderwasm.lookup.ElementLookups;

public final class ClassEmitContext {
    private final ElementLookups lookups;
    private final ClassFileEmitter emitter;
    private final WasmGenerators generators;
    private final WasmAssemblerConfiguration configuration;
    private final MetadataStorage metadataStorage;

    public ClassEmitContext(
            ElementLookups lookups,
            ClassFileEmitter emitter,
            WasmGenerators generators,
            WasmAssemblerConfiguration configuration,
            MetadataStorage metadataStorage
    ) {
        this.lookups = lookups;
        this.emitter = emitter;
        this.generators = generators;
        this.configuration = configuration;
        this.metadataStorage = metadataStorage;
    }

    /**
     * Retrieves the lookups that are used to look up elements.
     *
     * @return the lookups
     */
    public ElementLookups getLookups() {
        return lookups;
    }

    /**
     * Retrieves the emitter that is used to emit class files.
     *
     * @return the emitter
     */
    public ClassFileEmitter getEmitter() {
        return emitter;
    }

    /**
     * Retrieves the generators that are used to generate code.
     *
     * @return the generators
     */
    public WasmGenerators getGenerators() {
        return generators;
    }

    /**
     * Retrieves the configuration of the assembler.
     *
     * @return the configuration of the assembler
     */
    public WasmAssemblerConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Retrieves the metadata storage associated with this context.
     *
     * @return the metadata storage
     */
    public MetadataStorage getMetadataStorage() {
        return metadataStorage;
    }
}
