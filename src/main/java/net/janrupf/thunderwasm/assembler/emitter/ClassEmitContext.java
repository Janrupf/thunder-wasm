package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.lookup.ElementLookups;

public final class ClassEmitContext {
    private final ElementLookups lookups;
    private final ClassFileEmitter emitter;
    private final WasmGenerators generators;

    public ClassEmitContext(ElementLookups lookups, ClassFileEmitter emitter, WasmGenerators generators) {
        this.lookups = lookups;
        this.emitter = emitter;
        this.generators = generators;
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
}
