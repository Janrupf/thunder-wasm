package net.janrupf.thunderwasm.assembler;

import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.assembler.part.FunctionAssembler;
import net.janrupf.thunderwasm.data.Global;
import net.janrupf.thunderwasm.instructions.Function;
import net.janrupf.thunderwasm.instructions.Local;
import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.encoding.LargeIntArray;
import net.janrupf.thunderwasm.module.section.*;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.ValueType;

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
        // Create global constructors
        GlobalSection globalSection = lookups.requireSingleSection(GlobalSection.class, (byte) 0x06);
        LargeArray<Global> globals = globalSection.getGlobals();

        for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(globals.largeLength()) < 0; i = i.add(1)) {
            emitGlobalInitializer(globals.get(i), determineMethodName("global", i));
        }

        MethodEmitter constructor = this.emitter.method(
                "<init>",
                Visibility.PUBLIC,
                false,
                false,
                PrimitiveType.VOID,
                new JavaType[0],
                new JavaType[0]
        );

        // Emit a call to the super constructor
        CodeEmitter code = constructor.code();
        code.loadThis();
        code.invoke(ObjectType.OBJECT, "<init>", new JavaType[0], PrimitiveType.VOID, InvokeType.SPECIAL, false);

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

            // Create an isolated emit context
            WasmFrameState frameState = new WasmFrameState(
                    new ValueType[0],
                    Collections.emptyList()
            );

            frameState.pushOperand(global.getType().getValueType());

            // Emit the setter
            generators.getGlobalGenerator().emitSetGlobal(i, global, new CodeEmitContext(
                    lookups,
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

        code.doReturn(PrimitiveType.VOID);
        code.finish(1 + maxOperands, maxLocals);
    }

    private void emitGlobalInitializer(
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

    private void processSection(WasmSection section) throws WasmAssemblerException {
        if (section instanceof CustomSection) {
            // Nothing to do
            return;
        }

        if (section instanceof GlobalSection) {
            processGlobalSection((GlobalSection) section);
        } else if (section instanceof CodeSection) {
            processCodeSection((CodeSection) section);
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
        LargeArray<FunctionType> functionTypes = lookups.requireSingleSection(TypeSection.class, (byte) 0x01).getTypes();
        LargeIntArray functionTypesIndices = lookups.requireSingleSection(FunctionSection.class, (byte) 0x03).getTypes();

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
