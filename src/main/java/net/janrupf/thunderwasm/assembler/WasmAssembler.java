package net.janrupf.thunderwasm.assembler;

import net.janrupf.thunderwasm.ThunderWasmException;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.assembler.generator.TableGenerator;
import net.janrupf.thunderwasm.data.Global;
import net.janrupf.thunderwasm.eval.EvalContext;
import net.janrupf.thunderwasm.exports.Export;
import net.janrupf.thunderwasm.imports.Import;
import net.janrupf.thunderwasm.imports.MemoryImportDescription;
import net.janrupf.thunderwasm.imports.TableImportDescription;
import net.janrupf.thunderwasm.imports.TypeImportDescription;
import net.janrupf.thunderwasm.instructions.Expr;
import net.janrupf.thunderwasm.instructions.Function;
import net.janrupf.thunderwasm.lookup.ElementLookups;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.lookup.ModuleLookups;
import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.*;
import net.janrupf.thunderwasm.module.section.segment.DataSegment;
import net.janrupf.thunderwasm.module.section.segment.DataSegmentMode;
import net.janrupf.thunderwasm.module.section.segment.ElementSegment;
import net.janrupf.thunderwasm.module.section.segment.ElementSegmentMode;
import net.janrupf.thunderwasm.runtime.ImportedGlobalValueReference;
import net.janrupf.thunderwasm.runtime.UnresolvedFunctionReference;
import net.janrupf.thunderwasm.types.*;

import java.util.Collections;
import java.util.EnumSet;

/**
 * Entry point for translating a {@link WasmModule} into java bytecode.
 */
public final class WasmAssembler {
    private final String packageName;
    private final String className;

    private final WasmModule module;
    private final ClassFileEmitter emitter;
    private final ModuleLookups lookups;
    private final ElementLookups elementLookups;
    private final EnumSet<ProcessedSections> onceProcessedSections;

    private final ClassEmitContext classEmitContext;

    private final WasmGenerators generators;

    public WasmAssembler(
            WasmModule module,
            ClassFileEmitterFactory emitterFactory,
            String packageName,
            String className,
            WasmGenerators generators
    ) {
        this.packageName = packageName;
        this.className = className;

        this.module = module;
        this.emitter = emitterFactory.createFor(
                packageName,
                className,
                ObjectType.OBJECT,
                Collections.singletonList(generators.getExportGenerator().getExportInterface())
        );

        this.lookups = new ModuleLookups(module);
        this.elementLookups = new ElementLookups(lookups);
        this.onceProcessedSections = EnumSet.noneOf(ProcessedSections.class);

        this.classEmitContext = new ClassEmitContext(
                elementLookups,
                emitter,
                generators
        );

        this.generators = generators;
    }

    /**
     * Retrieves the package name of the class that will be generated.
     *
     * @return the package name
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Retrieves the class name of the class that will be generated.
     *
     * @return the class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Assembles the module into a java class.
     *
     * @return the assembled class
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    public byte[] assembleToModule() throws WasmAssemblerException {
        if (module.getVersion() != 1) {
            throw new IllegalArgumentException("Unsupported WASM version: " + module.getVersion());
        }

        this.emitStaticConstructor();
        this.emitConstructor();

        for (WasmSection section : module.getSections()) {
            processSection(section);
        }

        return emitter.finish();
    }

    /**
     * Emit the static constructor for the class.
     */
    private void emitStaticConstructor() throws WasmAssemblerException {
        // Create the static initializer
        MethodEmitter staticConstructor = emitter.method(
                "<clinit>",
                Visibility.PRIVATE,
                true,
                false,
                PrimitiveType.VOID,
                Collections.emptyList(),
                Collections.emptyList()
        );

        CodeEmitter code = staticConstructor.code();

        code.doReturn();
        code.finish();
        staticConstructor.finish();
    }

    /**
     * Emit the constructor for the class.
     */
    private void emitConstructor() throws WasmAssemblerException {
        WasmFrameState frameState = new WasmFrameState(
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                null
        );

        // Create the import fields
        ImportSection importSection = lookups.findSingleSection(ImportSection.LOCATOR);
        if (importSection != null) {
            for (Import<?> im : importSection.getImports()) {
                generators.getImportGenerator().addImport(im, emitter);
            }
        }

        // Create global constructors
        GlobalSection globalSection = lookups.findSingleSection(GlobalSection.LOCATOR);

        // Create element segments
        ElementSection elementSection = lookups.findSingleSection(ElementSection.LOCATOR);
        if (elementSection != null) {
            for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(elementSection.getSegments().largeLength()) < 0; i = i.add(1)) {
                generators.getTableGenerator().addElementSegment(i, elementSection.getSegments().get(i), emitter);
            }
        }

        // Create tables
        TableSection tableSection = lookups.findSingleSection(TableSection.LOCATOR);
        if (tableSection != null) {
            for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(tableSection.getTypes().largeLength()) < 0; i = i.add(1)) {
                generators.getTableGenerator().addTable(i, tableSection.getTypes().get(i), emitter);
            }
        }

        MethodEmitter constructor = this.emitter.method(
                "<init>",
                Visibility.PUBLIC,
                false,
                false,
                PrimitiveType.VOID,
                Collections.singletonList(generators.getImportGenerator().getLinkerType()),
                Collections.singletonList(ObjectType.of(ThunderWasmException.class))
        );

        CodeEmitter code = constructor.code();

        LocalVariables localVariables = new LocalVariables(constructor.getThisLocal(), null);

        // Emit a call to the super constructor
        code.loadLocal(localVariables.getThis());
        code.invoke(ObjectType.OBJECT, "<init>", new JavaType[0], PrimitiveType.VOID, InvokeType.SPECIAL, false);

        CodeEmitContext emitContext = new CodeEmitContext(
                "$init$block$",
                null,
                classEmitContext.getEmitter(),
                code,
                elementLookups,
                frameState,
                generators,
                localVariables
        );

        // Initializes imports if any
        if (importSection != null) {
            for (Import<?> im : importSection.getImports()) {
                // The contract for emitLinkImport says the linker should be on top of the stack
                code.loadLocal(constructor.getArgumentLocals().get(0));

                generators.getImportGenerator().emitLinkImport(im, emitContext);
            }
        }

        // Initializes globals if any
        if (globalSection != null) {
            emitGlobalInitializers(globalSection, emitContext);
        }

        // Initializes tables if any
        if (tableSection != null) {
            for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(tableSection.getTypes().largeLength()) < 0; i = i.add(1)) {
                generators.getTableGenerator().emitTableConstructor(i, tableSection.getTypes().get(i), emitContext);
            }
        }

        // Initializes element segments if any
        if (elementSection != null) {
            for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(elementSection.getSegments().largeLength()) < 0; i = i.add(1)) {
                ElementSegment segment = elementSection.getSegments().get(i);

                // Evaluate the segment init values
                Expr[] initExprs = segment.getInit().asFlatArray();
                if (initExprs == null) {
                    throw new WasmAssemblerException("Element segment init values are too large");
                }

                Object[] initValues = new Object[initExprs.length];
                for (int j = 0; j < initExprs.length; j++) {
                    EvalContext evalContext = new EvalContext(emitContext.getLookups());
                    initValues[j] = evalContext.evalSingleValue(initExprs[j], true, segment.getType());
                }

                generators.getTableGenerator().emitElementSegmentConstructor(
                        i,
                        segment,
                        initValues,
                        emitContext
                );

                if (segment.getMode() instanceof ElementSegmentMode.Active) {
                    ElementSegmentMode.Active activeMode = (ElementSegmentMode.Active) segment.getMode();

                    EvalContext evalContext = new EvalContext(emitContext.getLookups());

                    // Evaluate the table offset expression
                    int tableOffset = (int) evalContext.evalSingleValue(
                            activeMode.getTableOffset(),
                            true,
                            NumberType.I32
                    );

                    FoundElement<TableType, TableImportDescription> table = elementLookups.requireTable(
                            LargeArrayIndex.ZERO.add(activeMode.getTableIndex()));

                    TableType tableType;
                    if (table.isImport()) {
                        generators.getImportGenerator().emitLoadTableReference(
                                table.getImport(),
                                emitContext
                        );
                        tableType = table.getImport().getDescription().getType();
                    } else {
                        generators.getTableGenerator().emitLoadTableReference(
                                table.getIndex(),
                                emitContext
                        );
                        tableType = table.getElement();
                    }


                    code.loadConstant(tableOffset);
                    code.loadConstant(0);
                    code.loadConstant((int) segment.getInit().length());

                    TableGenerator tableGenerator = generators.getTableGenerator();
                    tableGenerator.emitTableInit(tableType, i, segment, emitContext);
                    tableGenerator.emitDropElement(i, segment, emitContext);
                }
            }
        }

        // Initialize memories if any
        MemorySection memorySection = lookups.findSingleSection(MemorySection.LOCATOR);
        if (memorySection != null) {
            for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(memorySection.getTypes().largeLength()) < 0; i = i.add(1)) {
                generators.getMemoryGenerator().emitMemoryConstructor(i, memorySection.getTypes().get(i), emitContext);
            }
        }

        // Initializes data segments if any
        DataSection dataSection = lookups.findSingleSection(DataSection.LOCATOR);
        if (dataSection != null) {
            for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(dataSection.getSegments().largeLength()) < 0; i = i.add(1)) {
                DataSegment segment = dataSection.getSegments().get(i);
                generators.getMemoryGenerator().emitDataSegmentConstructor(i, segment, emitContext);

                if (segment.getMode() instanceof DataSegmentMode.Active) {
                    DataSegmentMode.Active activeMode = (DataSegmentMode.Active) segment.getMode();

                    EvalContext evalContext = new EvalContext(emitContext.getLookups());

                    // Evaluate the memory offset expression
                    int memoryOffset = (int) evalContext.evalSingleValue(
                            activeMode.getMemoryOffset(),
                            true,
                            NumberType.I32
                    );

                    FoundElement<MemoryType, MemoryImportDescription> memory = elementLookups.requireMemory(
                            LargeArrayIndex.ZERO.add(activeMode.getMemoryIndex()));


                    code.loadConstant(memoryOffset);
                    code.loadConstant(0);
                    code.loadConstant((int) segment.getInit().length());

                    if (memory.isImport()) {
                        generators.getImportGenerator().emitMemoryInit(
                                memory.getImport(),
                                i,
                                segment,
                                emitContext
                        );
                    } else {
                        generators.getMemoryGenerator().emitMemoryInit(
                                memory.getIndex(),
                                memory.getElement(),
                                i,
                                segment,
                                emitContext
                        );
                    }
                }
            }
        }

        // Call the start function if it exists
        StartSection startSection = lookups.findSingleSection(StartSection.LOCATOR);
        if (startSection != null) {
            int startFunctionIndex = startSection.getIndex();
            FoundElement<Integer, TypeImportDescription> functionTypeIndex = elementLookups.requireFunctionTypeIndex(
                    LargeArrayIndex.fromU32(startFunctionIndex));
            FunctionType functionType = elementLookups.resovleFunctionType(functionTypeIndex);

            if (functionTypeIndex.isImport()) {
                generators.getImportGenerator().emitInvokeFunction(
                        functionTypeIndex.getImport(),
                        emitContext
                );
            } else {
                generators.getFunctionGenerator().emitInvokeFunction(
                        functionTypeIndex.getIndex(),
                        functionType,
                        emitContext
                );
            }
        }

        code.doReturn();
        code.finish();
    }

    private void emitGlobalInitializers(
            GlobalSection section,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        LargeArray<Global> globals = section.getGlobals();

        for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(globals.largeLength()) < 0; i = i.add(1)) {
            Global global = globals.get(i);

            EvalContext evalContext = new EvalContext(elementLookups);
            Object globalValue = evalContext.evalSingleValue(global.getInit(), true, global.getType().getValueType());

            // Emit the setter
            if (globalValue instanceof UnresolvedFunctionReference) {
                int functionIndex = ((UnresolvedFunctionReference) globalValue).getFunctionIndex();
                FoundElement<Integer, TypeImportDescription> functionTypeIndex = context.getLookups().requireFunctionTypeIndex(
                        LargeArrayIndex.fromU32(functionIndex));
                FunctionType functionType = context.getLookups().resovleFunctionType(functionTypeIndex);

                if (functionTypeIndex.isImport()) {
                    context.getGenerators().getImportGenerator().emitLoadFunctionReference(functionTypeIndex.getImport(), context);
                } else {
                    context.getGenerators().getFunctionGenerator().emitLoadFunctionReference(functionTypeIndex.getIndex(), functionType, context);
                }
            } else if (globalValue instanceof ImportedGlobalValueReference) {
                context.getGenerators().getImportGenerator().emitGetGlobal(
                        ((ImportedGlobalValueReference) globalValue).getImportDescription(),
                        context
                );
            } else {
                context.getEmitter().loadConstant(globalValue);
            }

            generators.getGlobalGenerator().emitSetGlobal(i, global, context);
        }
    }

    private void processSection(WasmSection section) throws WasmAssemblerException {
        if (section instanceof CustomSection) {
            // Nothing to do
            return;
        }

        if (section instanceof GlobalSection) {
            processGlobalSection((GlobalSection) section);
        } else if (section instanceof CodeSection) {
            processCodeSection((CodeSection) section);
        } else if (section instanceof MemorySection) {
            processMemorySection((MemorySection) section);
        } else if (section instanceof DataSection) {
            processDataSection((DataSection) section);
        } else if (section instanceof ExportSection) {
            processExportSection((ExportSection) section);
        }
    }

    /**
     * Processes a global section.
     *
     * @param section the global section to process
     * @throws WasmAssemblerException if an error occurs during processing
     */
    private void processGlobalSection(GlobalSection section) throws WasmAssemblerException {
        // There can only ever be one global section
        markAsProcessed(ProcessedSections.GLOBAL);

        LargeArray<Global> globals = section.getGlobals();
        for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(globals.largeLength()) < 0; i = i.add(1)) {
            generators.getGlobalGenerator().addGlobal(i, globals.get(i), emitter);
        }
    }

    /**
     * Processes a code section.
     *
     * @param section the code section to process
     * @throws WasmAssemblerException if an error occurs during processing
     */
    private void processCodeSection(CodeSection section) throws WasmAssemblerException {
        // There can only ever be one code section
        markAsProcessed(ProcessedSections.CODE);

        LargeArray<Function> functions = section.getFunctions();
        for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(functions.largeLength()) < 0; i = i.add(1)) {
            processFunction(i, functions.get(i), classEmitContext);
        }
    }

    /**
     * Process a memory section.
     *
     * @param section the memory section to process
     * @throws WasmAssemblerException if an error occurs during processing
     */
    private void processMemorySection(MemorySection section) throws WasmAssemblerException {
        // There can only ever be one memory section
        markAsProcessed(ProcessedSections.MEMORY);

        LargeArray<MemoryType> memories = section.getTypes();
        for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(memories.largeLength()) < 0; i = i.add(1)) {
            generators.getMemoryGenerator().addMemory(i, memories.get(i), emitter);
        }
    }

    private void processDataSection(DataSection section) throws WasmAssemblerException {
        // There can only ever be one data section
        markAsProcessed(ProcessedSections.DATA);

        LargeArray<DataSegment> segments = section.getSegments();
        for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(segments.largeLength()) < 0; i = i.add(1)) {
            DataSegment segment = segments.get(i);
            generators.getMemoryGenerator().addDataSegment(i, segment, emitter);
        }
    }

    private void processExportSection(ExportSection section) throws WasmAssemblerException {
        markAsProcessed(ProcessedSections.EXPORT);

        LargeArray<Export<?>> exports = section.getExports();
        for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(exports.largeLength()) < 0; i = i.add(1)) {
            generators.getExportGenerator().addExport(i, exports.get(i), classEmitContext);
        }

        generators.getExportGenerator().emitExportImplementation(exports, classEmitContext);
    }

    /**
     * Processes a function.
     *
     * @param index    the index of the function being processed
     * @param function the function to process
     * @throws WasmAssemblerException if an error occurs during processing
     */
    private void processFunction(LargeArrayIndex index, Function function, ClassEmitContext context) throws WasmAssemblerException {
        generators.getFunctionGenerator().addFunction(index, function, context);
    }

    /**
     * Marks a section as processed.
     * <p>
     * This method will throw an {@link IllegalStateException} if the section was already processed.
     *
     * @param section the section to mark as processed
     */
    private void markAsProcessed(ProcessedSections section) {
        if (onceProcessedSections.contains(section)) {
            throw new IllegalStateException("Section " + section + " was already processed");
        }

        onceProcessedSections.add(section);
    }
}
