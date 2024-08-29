package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.WasmFrameState;

/**
 * Represents the context in which code is emitted.
 */
public final class CodeEmitContext {
    private final CodeEmitter emitter;
    private final WasmFrameState frameState;

    public CodeEmitContext(CodeEmitter emitter, WasmFrameState frameState) {
        this.emitter = emitter;
        this.frameState = frameState;
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
}
