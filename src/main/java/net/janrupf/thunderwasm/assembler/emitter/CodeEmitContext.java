package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.lookup.ElementLookups;
import net.janrupf.thunderwasm.assembler.WasmFrameState;

/**
 * Represents the context in which code is emitted.
 */
public final class CodeEmitContext {
    private final ElementLookups lookups;
    private final CodeEmitter emitter;
    private final WasmFrameState frameState;
    private final WasmGenerators generators;

    public CodeEmitContext(
            ElementLookups lookups,
            CodeEmitter emitter,
            WasmFrameState frameState,
            WasmGenerators generators
    ) {
        this.lookups = lookups;
        this.emitter = emitter;
        this.frameState = frameState;
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
     * Retrieves the emitter that is used to emit code.
     *
     * @return the emitter
     */
    public CodeEmitter getEmitter() {
        return emitter;
    }

    /**
     * Retrieves the frame state of the context.
     *
     * @return the frame state
     */
    public WasmFrameState getFrameState() {
        return frameState;
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
