package net.janrupf.thunderwasm.assembler;

import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.assembler.part.FunctionAssembler;
import net.janrupf.thunderwasm.instructions.Function;
import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.encoding.LargeIntArray;
import net.janrupf.thunderwasm.module.section.*;
import net.janrupf.thunderwasm.types.FunctionType;

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

    public WasmAssembler(
            WasmModule module,
            ClassFileEmitterFactory emitterFactory,
            String packageName,
            String className
    ) {
        this.packageName = packageName;
        this.className = className;

        this.module = module;
        this.emitter = emitterFactory.createFor(packageName, className);

        this.lookups = new ModuleLookups(module);
        this.onceProcessedSections = EnumSet.noneOf(ProcessedSections.class);
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
        MethodEmitter constructor = this.emitter.method(
                "<init>",
                Visibility.PUBLIC,
                false,
                false,
                PrimitiveType.VOID,
                new JavaType[0],
                new JavaType[0]
        );

        // Emit an empty constructor
        CodeEmitter code = constructor.code();
        code.loadThis();
        code.invoke(ObjectType.OBJECT, "<init>", new JavaType[0], PrimitiveType.VOID, InvokeType.SPECIAL, false);
        code.doReturn(PrimitiveType.VOID);
        code.finish(1, 0);
    }

    private void processSection(WasmSection section) throws WasmAssemblerException {
        if (section instanceof CustomSection) {
            // Nothing to do
            return;
        }

        if (section instanceof CodeSection) {
            processCodeSection((CodeSection) section);
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
                functionType.getOutputs()
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
