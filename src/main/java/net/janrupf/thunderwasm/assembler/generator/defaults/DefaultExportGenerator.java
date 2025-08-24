package net.janrupf.thunderwasm.assembler.generator.defaults;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.data.MetadataKey;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.assembler.generator.ExportGenerator;
import net.janrupf.thunderwasm.data.Global;
import net.janrupf.thunderwasm.exports.*;
import net.janrupf.thunderwasm.imports.GlobalImportDescription;
import net.janrupf.thunderwasm.imports.MemoryImportDescription;
import net.janrupf.thunderwasm.imports.TableImportDescription;
import net.janrupf.thunderwasm.imports.TypeImportDescription;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.runtime.WasmModuleExports;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.MemoryType;
import net.janrupf.thunderwasm.types.TableType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DefaultExportGenerator implements ExportGenerator {
    @Override
    public void addExport(LargeArrayIndex i, Export<?> export, ClassEmitContext context) throws WasmAssemblerException {
        Export<FunctionExportDescription> functionExport = export.tryCast(FunctionExportDescription.class);
        if (functionExport != null) {
            addFunctionExport(functionExport, context);
            return;
        }

        Export<MemoryExportDescription> memoryExport = export.tryCast(MemoryExportDescription.class);
        if (memoryExport != null) {
            addMemoryExport(memoryExport, context);
            return;
        }

        Export<TableExportDescription> tableExport = export.tryCast(TableExportDescription.class);
        if (tableExport != null) {
            addTableExport(tableExport, context);
            return;
        }

        Export<GlobalExportDescription> globalExport = export.tryCast(GlobalExportDescription.class);
        if (globalExport != null) {
            addGlobalExport(globalExport, context);
            return;
        }

        throw new WasmAssemblerException("Unsupported export type " + export.getDescription());
    }

    private void addFunctionExport(Export<FunctionExportDescription> export, ClassEmitContext context)
            throws WasmAssemblerException {
        LargeArrayIndex exportedFunctionIndex = LargeArrayIndex.fromU32(export.getDescription().getIndex());
        FoundElement<Integer, TypeImportDescription> foundFunction =
                context.getLookups().requireFunctionTypeIndex(exportedFunctionIndex);

        FunctionType functionType = context.getLookups().resovleFunctionType(foundFunction);

        if (foundFunction.isImport()) {
            context.getGenerators().getImportGenerator().makeFunctionExportable(
                    foundFunction.getImport(),
                    context
            );
        } else {
            context.getGenerators().getFunctionGenerator().makeFunctionExportable(
                    foundFunction.getIndex(),
                    functionType,
                    context
            );
        }
    }

    private void addMemoryExport(Export<MemoryExportDescription> export, ClassEmitContext context)
            throws WasmAssemblerException {
        FoundElement<MemoryType, MemoryImportDescription> memory = context.getLookups()
                .requireMemory(LargeArrayIndex.fromU32(export.getDescription().getIndex()));

        if (memory.isImport()) {
            context.getGenerators().getImportGenerator().makeMemoryExportable(
                    memory.getImport(),
                    context
            );
        } else {
            context.getGenerators().getMemoryGenerator().makeMemoryExportable(
                    memory.getIndex(),
                    memory.getElement(),
                    context
            );
        }
    }

    private void addTableExport(Export<TableExportDescription> export, ClassEmitContext context)
            throws WasmAssemblerException {
        FoundElement<TableType, TableImportDescription> table = context.getLookups()
                .requireTable(LargeArrayIndex.fromU32(export.getDescription().getIndex()));

        if (table.isImport()) {
            context.getGenerators().getImportGenerator().makeTableExportable(
                    table.getImport(),
                    context
            );
        } else {
            context.getGenerators().getTableGenerator().makeTableExportable(
                    table.getIndex(),
                    table.getElement(),
                    context
            );
        }
    }

    private void addGlobalExport(Export<GlobalExportDescription> globalExport, ClassEmitContext context)
            throws WasmAssemblerException {
        FoundElement<Global, GlobalImportDescription> global = context.getLookups()
                .requireGlobal(LargeArrayIndex.fromU32(globalExport.getDescription().getIndex()));

        if (global.isImport()) {
            context.getGenerators().getImportGenerator().makeGlobalExportable(
                    global.getImport(),
                    context
            );
        } else {
            context.getGenerators().getGlobalGenerator().makeGlobalExportable(
                    global.getIndex(),
                    global.getElement(),
                    context
            );
        }
    }


    @Override
    public void emitExportImplementation(LargeArray<Export<?>> exports, ClassEmitContext context)
            throws WasmAssemblerException {

        MethodEmitter methodEmitter = context.getEmitter().method(
                "getExports",
                Visibility.PUBLIC,
                false,
                true,
                ObjectType.of(Map.class),
                Collections.emptyList(),
                Collections.emptyList()
        );

        CodeEmitter emitter = methodEmitter.code();
        JavaLocal thisLocal = methodEmitter.getThisLocal();

        CodeEmitContext codeEmitContext = new CodeEmitContext(
                null,
                null,
                context.getEmitter(),
                emitter,
                context.getLookups(),
                new WasmFrameState(),
                context.getGenerators(),
                new LocalVariables(thisLocal, null, null),
                context.getConfiguration()
        );

        emitter.doNew(ObjectType.of(HashMap.class));
        emitter.duplicate();
        emitter.invoke(
                ObjectType.of(HashMap.class),
                "<init>",
                new JavaType[0],
                PrimitiveType.VOID,
                InvokeType.SPECIAL,
                false
        );

        for (Export<?> export : exports) {
            emitter.duplicate();
            emitter.loadConstant(export.getName());

            Export<FunctionExportDescription> functionExport = export.tryCast(FunctionExportDescription.class);
            if (functionExport != null) {
                emitFunctionExport(functionExport.getDescription(), codeEmitContext);
            }

            Export<MemoryExportDescription> memoryExport = export.tryCast(MemoryExportDescription.class);
            if (memoryExport != null) {
                emitMemoryExport(memoryExport.getDescription(), codeEmitContext);
            }

            Export<TableExportDescription> tableExport = export.tryCast(TableExportDescription.class);
            if (tableExport != null) {
                emitTableExport(tableExport.getDescription(), codeEmitContext);
            }

            Export<GlobalExportDescription> globalExport = export.tryCast(GlobalExportDescription.class);
            if (globalExport != null) {
                emitGlobalExport(globalExport.getDescription(), codeEmitContext);
            }

            emitter.invoke(
                    ObjectType.of(Map.class),
                    "put",
                    new JavaType[]{ObjectType.OBJECT, ObjectType.OBJECT},
                    ObjectType.OBJECT,
                    InvokeType.INTERFACE,
                    true
            );
            emitter.pop();
        }

        emitter.doReturn();
        emitter.finish();
        methodEmitter.finish();
    }

    private void emitFunctionExport(FunctionExportDescription export, CodeEmitContext context)
            throws WasmAssemblerException {
        LargeArrayIndex exportedFunctionIndex = LargeArrayIndex.fromU32(export.getIndex());
        FoundElement<Integer, TypeImportDescription> foundFunction =
                context.getLookups().requireFunctionTypeIndex(exportedFunctionIndex);

        FunctionType functionType = context.getLookups().resovleFunctionType(foundFunction);

        if (foundFunction.isImport()) {
            context.getGenerators().getImportGenerator().emitLoadFunctionExport(
                    foundFunction.getImport(),
                    context
            );
        } else {
            context.getGenerators().getFunctionGenerator().emitLoadFunctionExport(
                    foundFunction.getIndex(),
                    functionType,
                    context
            );
        }
    }

    private void emitMemoryExport(MemoryExportDescription export, CodeEmitContext context)
            throws WasmAssemblerException {
        FoundElement<MemoryType, MemoryImportDescription> memory = context.getLookups()
                .requireMemory(LargeArrayIndex.fromU32(export.getIndex()));

        if (memory.isImport()) {
            context.getGenerators().getImportGenerator().emitLoadMemoryExport(
                    memory.getImport(),
                    context
            );
        } else {
            context.getGenerators().getMemoryGenerator().emitLoadMemoryExport(
                    memory.getIndex(),
                    memory.getElement(),
                    context
            );
        }
    }

    private void emitTableExport(TableExportDescription export, CodeEmitContext context)
            throws WasmAssemblerException {
        FoundElement<TableType, TableImportDescription> table = context.getLookups()
                .requireTable(LargeArrayIndex.fromU32(export.getIndex()));

        if (table.isImport()) {
            context.getGenerators().getImportGenerator().emitLoadTableExport(
                    table.getImport(),
                    context
            );
        } else {
            context.getGenerators().getTableGenerator().emitLoadTableExport(
                    table.getIndex(),
                    table.getElement(),
                    context
            );
        }
    }

    private void emitGlobalExport(GlobalExportDescription export, CodeEmitContext context)
        throws WasmAssemblerException {
        FoundElement<Global, GlobalImportDescription> global = context.getLookups()
                .requireGlobal(LargeArrayIndex.fromU32(export.getIndex()));

        if (global.isImport()) {
            context.getGenerators().getImportGenerator().emitLoadGlobalExport(
                    global.getImport(),
                    context
            );
        } else {
            context.getGenerators().getGlobalGenerator().emitLoadGlobalExport(
                    global.getIndex(),
                    global.getElement(),
                    context
            );
        }
    }

    @Override
    public ObjectType getExportInterface() {
        return ObjectType.of(WasmModuleExports.class);
    }
}
