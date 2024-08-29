package net.janrupf.thunderwasm.assembler.emitter;

public interface MethodEmitter {
    /**
     * Returns the code emitter for this method.
     *
     * @return the code emitter
     */
    CodeEmitter code();

    /**
     * Finalizes the method.
     */
    void finish();
}
