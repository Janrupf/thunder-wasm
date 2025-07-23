package net.janrupf.thunderwasm.assembler.generator;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.ClassEmitContext;
import net.janrupf.thunderwasm.instructions.Function;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;

public interface FunctionGenerator {
    /**
     * Add a function to the generated class.
     *
     * @param i        the index of the function
     * @param function the function to add
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void addFunction(LargeArrayIndex i, Function function, ClassEmitContext context) throws WasmAssemblerException;
}
