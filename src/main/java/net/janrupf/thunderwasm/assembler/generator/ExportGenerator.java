package net.janrupf.thunderwasm.assembler.generator;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.ClassEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.exports.Export;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;

public interface ExportGenerator {
    /**
     * Add an export to the module.
     *
     * @param i       the index of the export
     * @param export  the export
     * @param context the context to use
     * @throws WasmAssemblerException if the export could not be added
     */
    void addExport(LargeArrayIndex i, Export<?> export, ClassEmitContext context) throws WasmAssemblerException;

    /**
     * Emits the code that implements the actual export process.
     *
     * @param exports the exports to generate
     * @param context the context to use
     * @throws WasmAssemblerException if the export function could not be generated
     */
    void emitExportImplementation(LargeArray<Export<?>> exports, ClassEmitContext context)
            throws WasmAssemblerException;

    /**
     * The interface type to add to the module.
     *
     * @return the export interface type
     */
    ObjectType getExportInterface();
}
