package net.janrupf.thunderwasm.assembler.generator;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmAssemblerStatistics;
import net.janrupf.thunderwasm.assembler.emitter.ClassEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.instructions.Function;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;

public interface FunctionGenerator {
    /**
     * Add the function lookup table to the generated class.
     *
     * @param statistics the statistics of the module
     * @param context    the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void addFunctionTable(WasmAssemblerStatistics statistics, ClassEmitContext context) throws WasmAssemblerException;

    /**
     * Add a function to the generated class.
     *
     * @param i        the index of the function
     * @param function the function to add
     * @param context  the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void addFunction(LargeArrayIndex i, Function function, ClassEmitContext context) throws WasmAssemblerException;

    /**
     * Emit the static initializer for the function table.
     *
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void emitStaticFunctionTableInitializer(CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emit the code required to initialize the function table.
     *
     * @param functionCount      the total number of functions in the table
     * @param firstLocalFunction the first index of a local (non-imported) function
     * @param context            the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void emitFunctionTableInitializer(int functionCount, int firstLocalFunction, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emit the code that loads the function table.
     *
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void emitLoadFunctionTable(CodeEmitContext context) throws WasmAssemblerException;
}
