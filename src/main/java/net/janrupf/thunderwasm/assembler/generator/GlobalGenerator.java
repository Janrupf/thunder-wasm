package net.janrupf.thunderwasm.assembler.generator;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.ClassEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.ClassFileEmitter;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.data.Global;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;

public interface GlobalGenerator {
    /**
     * Add a global variable to the generated class.
     *
     * @param index   the index of the global variable
     * @param global  the global variable
     * @param emitter the emitter to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void addGlobal(LargeArrayIndex index, Global global, ClassFileEmitter emitter) throws WasmAssemblerException;

    /**
     * Emit the code to get a global variable.
     *
     * @param index   the index of the global variable
     * @param global  the global variable
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void emitGetGlobal(LargeArrayIndex index, Global global, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Emit the code to set a global variable.
     *
     * @param index   the index of the global variable
     * @param global  the global variable
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    void emitSetGlobal(LargeArrayIndex index, Global global, CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Make a global exportable.
     *
     * @param index   the index of the global variable
     * @param global  the global variable
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void makeGlobalExportable(LargeArrayIndex index, Global global, ClassEmitContext context)
            throws WasmAssemblerException;

    /**
     * Emit the code to load an export of a global variable.
     *
     * @param index   the index of the global variable to load
     * @param global  the global variable
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    void emitLoadGlobalExport(LargeArrayIndex index, Global global, CodeEmitContext context)
            throws WasmAssemblerException;
}
