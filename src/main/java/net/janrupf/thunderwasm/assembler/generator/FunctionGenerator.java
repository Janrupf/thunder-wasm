package net.janrupf.thunderwasm.assembler.generator;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmAssemblerStatistics;
import net.janrupf.thunderwasm.assembler.emitter.ClassEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.instructions.Function;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.FunctionType;

public interface FunctionGenerator {
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
     * Emit the code to invoke a module local function by its index.
     *
     * @param functionIndex the index of the function to invoke (of only local functions)
     * @param function      the function type
     * @param context       the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void emitInvokeFunction(LargeArrayIndex functionIndex, FunctionType function, CodeEmitContext context)
            throws WasmAssemblerException;

    /**
     * Emit the code to invoke a function indirectly via the function table.
     *
     * @param functionType the type of the function to invoke
     * @param tableIndex   the index of the function table to use
     * @param context      the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void emitInvokeFunctionIndirect(FunctionType functionType, LargeArrayIndex tableIndex, CodeEmitContext context)
            throws WasmAssemblerException;

    /**
     * Emit the code to load a function reference from the function table.
     *
     * @param i            the index of the function reference to load
     * @param functionType the type of the function reference to load
     * @param context      the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void emitLoadFunctionReference(LargeArrayIndex i, FunctionType functionType, CodeEmitContext context) throws WasmAssemblerException;
}
