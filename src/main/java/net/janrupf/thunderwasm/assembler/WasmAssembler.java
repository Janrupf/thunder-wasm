package net.janrupf.thunderwasm.assembler;

import net.janrupf.thunderwasm.ThunderWasmException;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.assembler.generator.TableGenerator;
import net.janrupf.thunderwasm.assembler.part.FunctionAssembler;
import net.janrupf.thunderwasm.data.Global;
import net.janrupf.thunderwasm.eval.EvalContext;
import net.janrupf.thunderwasm.imports.Import;
import net.janrupf.thunderwasm.imports.MemoryImportDescription;
import net.janrupf.thunderwasm.imports.TableImportDescription;
import net.janrupf.thunderwasm.instructions.Expr;
import net.janrupf.thunderwasm.instructions.Function;
import net.janrupf.thunderwasm.instructions.Local;
import net.janrupf.thunderwasm.lookup.ElementLookups;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.lookup.ModuleLookups;
import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.encoding.LargeIntArray;
import net.janrupf.thunderwasm.module.section.*;
import net.janrupf.thunderwasm.module.section.segment.DataSegment;
import net.janrupf.thunderwasm.module.section.segment.DataSegmentMode;
import net.janrupf.thunderwasm.module.section.segment.ElementSegment;
import net.janrupf.thunderwasm.module.section.segment.ElementSegmentMode;
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
        this.emitter = emitterFactory.createFor(packageName, className);

        this.lookups = new ModuleLookups(module);
        this.elementLookups = new ElementLookups(lookups);
        this.onceProcessedSections = EnumSet.noneOf(ProcessedSections.class);

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

        this.emitConstructor();

        for (WasmSection section : module.getSections()) {
            processSection(section);
        }

        return emitter.finish();
    }

    /**
     * Emit the constructor for the class.
     */
    private void emitConstructor() throws WasmAssemblerException {
        WasmFrameState frameState = new WasmFrameState(
                new ValueType[]{ReferenceType.OBJECT},
                Collections.emptyList()
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
        if (globalSection != null) {
            emitAllGlobalConstructor(globalSection);
        }

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
                new JavaType[]{generators.getImportGenerator().getLinkerType()},
                new JavaType[]{ObjectType.of(ThunderWasmException.class)}
        );

        // Emit a call to the super constructor
        CodeEmitter code = constructor.code();
        frameState.pushOperand(ReferenceType.OBJECT);
        code.loadThis();
        code.invoke(ObjectType.OBJECT, "<init>", new JavaType[0], PrimitiveType.VOID, InvokeType.SPECIAL, false);
        frameState.popOperand(ReferenceType.OBJECT);

        CodeEmitContext emitContext = new CodeEmitContext(
                elementLookups,
                code,
                frameState,
                generators
        );

        // Initializes imports if any
        if (importSection != null) {
            for (Import<?> im : importSection.getImports()) {
                // The contract for emitLinkImport says the linker should be on top of the stack
                code.loadLocal(0, generators.getImportGenerator().getLinkerType());
                frameState.pushOperand(ReferenceType.OBJECT);

                generators.getImportGenerator().emitLinkImport(im, emitContext);
            }
        }

        // Initializes globals if any
        if (globalSection != null) {
            emitGlobalInitializers(globalSection, code, frameState);
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

                    frameState.pushOperand(NumberType.I32);
                    frameState.pushOperand(NumberType.I32);
                    frameState.pushOperand(NumberType.I32);

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

                    frameState.pushOperand(NumberType.I32);
                    frameState.pushOperand(NumberType.I32);
                    frameState.pushOperand(NumberType.I32);

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

        code.doReturn(PrimitiveType.VOID);
        code.finish(frameState.getMaxOperandSlotCount(), frameState.getMaxLocalSlotCount());
    }

    private void emitAllGlobalConstructor(GlobalSection section) throws WasmAssemblerException {
        LargeArray<Global> globals = section.getGlobals();

        for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(globals.largeLength()) < 0; i = i.add(1)) {
            emitGlobalConstructor(globals.get(i), determineMethodName("global", i));
        }
    }

    private void emitGlobalConstructor(
            Global global,
            String name
    ) throws WasmAssemblerException {
        ValueType type = global.getType().getValueType();

        // Emit the initializer code
        FunctionAssembler assembler = new FunctionAssembler(
                lookups,
                generators,
                LargeArray.of(Local.class),
                global.getInit()
        );

        // Emit the initializer
        assembler.assemble(
                emitter,
                name,
                LargeArray.of(ValueType.class),
                LargeArray.of(ValueType.class, type),
                true
        );
    }

    private void emitGlobalInitializers(
            GlobalSection section,
            CodeEmitter code,
            WasmFrameState frameState
    ) throws WasmAssemblerException {
        LargeArray<Global> globals = section.getGlobals();

        int maxOperands = 0;
        int maxLocals = 0;

        for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(globals.largeLength()) < 0; i = i.add(1)) {
            Global global = globals.get(i);

            // Invoke the global initializers
            code.invoke(
                    emitter.getOwner(),
                    determineMethodName("global", i),
                    new JavaType[0],
                    WasmTypeConverter.toJavaType(global.getType().getValueType()),
                    InvokeType.STATIC,
                    false
            );

            frameState.pushOperand(global.getType().getValueType());

            // Emit the setter
            generators.getGlobalGenerator().emitSetGlobal(i, global, new CodeEmitContext(
                    elementLookups,
                    code,
                    frameState,
                    generators
            ));

            if (frameState.getMaxOperandSlotCount() > maxOperands) {
                maxOperands = frameState.getMaxOperandSlotCount();
            }

            if (frameState.getMaxLocalSlotCount() > maxLocals) {
                maxLocals = frameState.getMaxLocalSlotCount();
            }
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
            processFunction(i, functions.get(i));
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

    /**
     * Processes a function.
     *
     * @param index    the index of the function being processed
     * @param function the function to process
     * @throws WasmAssemblerException if an error occurs during processing
     */
    private void processFunction(LargeArrayIndex index, Function function) throws WasmAssemblerException {
        FunctionAssembler functionAssembler = new FunctionAssembler(
                lookups,
                generators,
                function.getLocals(),
                function.getExpr()
        );

        // Look up the function type
        LargeArray<FunctionType> functionTypes = lookups.requireSingleSection(TypeSection.LOCATOR).getTypes();
        LargeIntArray functionTypesIndices = lookups.requireSingleSection(FunctionSection.LOCATOR).getTypes();

        if (!functionTypesIndices.isValid(index)) {
            throw new WasmAssemblerException(
                    "Function in code section at index " + index + " has no associated index in function section"
            );
        }

        int functionTypeIndex = functionTypesIndices.get(index);
        if (!functionTypes.isValid(LargeArrayIndex.fromU64(functionTypeIndex))) {
            throw new WasmAssemblerException(
                    "Function in code section at index " + index + " has invalid function type index " + functionTypeIndex
            );
        }

        FunctionType functionType = functionTypes.get(LargeArrayIndex.fromU64(functionTypeIndex));

        // Emit the function
        functionAssembler.assemble(
                emitter,
                determineMethodName("code", index),
                functionType.getInputs(),
                functionType.getOutputs(),
                false
        );
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

    private String determineMethodName(String sectionName, LargeArrayIndex index) {
        return "$" + sectionName + "_" + index;
    }
}
