package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;

import java.util.List;

public interface MethodEmitter {
    /**
     * Retrieve the local representing the 'this' reference in non-static methods.
     *
     * @return the 'this' local variable, or null if the method is static
     */
    JavaLocal getThisLocal();

    /**
     * Retrieve the locals corresponding to the method arguments.
     *
     * @return the list of argument locals
     */
    List<JavaLocal> getArgumentLocals();

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
