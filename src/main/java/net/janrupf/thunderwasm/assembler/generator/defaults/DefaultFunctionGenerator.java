package net.janrupf.thunderwasm.assembler.generator.defaults;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmAssemblerStatistics;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.*;
import net.janrupf.thunderwasm.assembler.generator.FunctionGenerator;
import net.janrupf.thunderwasm.assembler.part.TranslatedFunctionSignature;
import net.janrupf.thunderwasm.instructions.Function;
import net.janrupf.thunderwasm.instructions.InstructionInstance;
import net.janrupf.thunderwasm.instructions.Local;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.lookup.ElementLookups;
import net.janrupf.thunderwasm.lookup.ModuleLookups;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.CodeSection;
import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.ValueType;
import net.janrupf.thunderwasm.util.ObjectUtil;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DefaultFunctionGenerator implements FunctionGenerator {
    private static final ObjectType LINKED_FUNCTION_TYPE = ObjectType.of(LinkedFunction.class);
    private static final ObjectType SIMPLE_LINKED_FUNCTION_TYPE = ObjectType.of(LinkedFunction.Simple.class);
    private static final ArrayType LINKED_FUNCTION_ARRAY_TYPE = new ArrayType(LINKED_FUNCTION_TYPE);

    @Override
    public void addFunctionTable(WasmAssemblerStatistics statistics, ClassEmitContext context) throws WasmAssemblerException {
        context.getEmitter().field(
                "functions",
                Visibility.PRIVATE,
                false,
                true,
                LINKED_FUNCTION_ARRAY_TYPE,
                null
        );

        context.getEmitter().field(
                "MODULE_FUNCTIONS",
                Visibility.PRIVATE,
                true,
                true,
                LINKED_FUNCTION_ARRAY_TYPE,
                null
        );

        // We use this chance to add a bunch of methods which populate the MODULE_FUNCTIONS array
        // The reason is pretty simple: There may be A LOT of function in the module, and we don't want to
        // hit the bytecode limit for a single method. So instead we split the initialization into multiple methods.
        ClassFileEmitter classEmitter = context.getEmitter();
        ElementLookups lookups = context.getLookups();
        ModuleLookups moduleLookups = lookups.getModuleLookups();

        CodeSection codeSection = moduleLookups.requireSingleSection(CodeSection.LOCATOR);

        if (codeSection.getFunctions().length() > Integer.MAX_VALUE) {
            throw new WasmAssemblerException("Function section has too many types");
        }

        int generatedInitializerCount = 0;
        int methodCount = 0;

        MethodEmitter currentMethodEmitter = null;
        CodeEmitter currentCodeEmitter = null;

        for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(codeSection.getFunctions().largeLength()) < 0; i = i.add(1)) {
            if (methodCount % 10000 == 0 && currentCodeEmitter != null) {
                currentCodeEmitter.doReturn();
                currentCodeEmitter.finish();
                currentMethodEmitter.finish();
                currentCodeEmitter = null;
            }

            if (currentCodeEmitter == null) {
                String methodName = determineFunctionTableInitializerName(generatedInitializerCount++);

                currentMethodEmitter = classEmitter.method(
                        methodName,
                        Visibility.PRIVATE,
                        true,
                        false,
                        PrimitiveType.VOID,
                        Collections.emptyList(),
                        Collections.emptyList()
                );
                currentCodeEmitter = currentMethodEmitter.code();
            }

            // Emit the function table initializer
            FunctionType type = determineFunctionType(i, lookups);
            TranslatedFunctionSignature signature = TranslatedFunctionSignature.of(type, classEmitter.getOwner());

            JavaMethodHandle handle = new JavaMethodHandle(
                    classEmitter.getOwner(),
                    determineMethodName(i),
                    signature.getJavaReturnType(),
                    signature.getJavaArgumentTypes(),
                    InvokeType.STATIC,
                    false
            );

            currentCodeEmitter.accessField(
                    currentCodeEmitter.getOwner(),
                    "MODULE_FUNCTIONS",
                    LINKED_FUNCTION_ARRAY_TYPE,
                    true,
                    false
            );
            currentCodeEmitter.loadConstant(i.getElementIndex());
            currentCodeEmitter.loadConstant(handle);
            currentCodeEmitter.loadConstant(signature.getOwnerArgumentIndex());
            currentCodeEmitter.invoke(
                    SIMPLE_LINKED_FUNCTION_TYPE,
                    "inferFromMethodHandle",
                    new JavaType[] { ObjectType.of(MethodHandle.class), PrimitiveType.INT },
                    SIMPLE_LINKED_FUNCTION_TYPE,
                    InvokeType.STATIC,
                    false
            );
            currentCodeEmitter.storeArrayElement();

            methodCount++;
        }

        if (currentCodeEmitter != null) {
            currentCodeEmitter.doReturn();
            currentCodeEmitter.finish();
            currentMethodEmitter.finish();
        }

        // And generate the final initializer method
        MethodEmitter finalMethodEmitter = classEmitter.method(
                "$initModuleFunctions",
                Visibility.PRIVATE,
                true,
                false,
                PrimitiveType.VOID,
                Collections.emptyList(),
                Collections.singletonList(ObjectType.of(WasmAssemblerException.class))
        );

        CodeEmitter finalCodeEmitter = finalMethodEmitter.code();

        // We need to initialize the MODULE_FUNCTIONS array
        finalCodeEmitter.loadConstant(statistics.getLocalFunctionCount());
        finalCodeEmitter.doNew(LINKED_FUNCTION_ARRAY_TYPE);
        finalCodeEmitter.accessField(
                finalCodeEmitter.getOwner(),
                "MODULE_FUNCTIONS",
                LINKED_FUNCTION_ARRAY_TYPE,
                true,
                true
        );

        for (int i = 0; i < generatedInitializerCount; i++) {
            finalCodeEmitter.invoke(
                    classEmitter.getOwner(),
                    determineFunctionTableInitializerName(i),
                    new JavaType[0],
                    PrimitiveType.VOID,
                    InvokeType.STATIC,
                    false
            );
        }

        finalCodeEmitter.doReturn();
        finalCodeEmitter.finish();
        finalMethodEmitter.finish();
    }

    @Override
    public void addFunction(LargeArrayIndex i, Function function, ClassEmitContext context) throws WasmAssemblerException {
        // Look up the function type
        FunctionType functionType = determineFunctionType(i, context.getLookups());
        ClassFileEmitter classEmitter = context.getEmitter();

        TranslatedFunctionSignature signature = TranslatedFunctionSignature.of(functionType, classEmitter.getOwner());
        LargeArray<Local> locals = function.getLocals();

        MethodEmitter methodEmitter = classEmitter.method(
                determineMethodName(i),
                // TODO: Exports...
                Visibility.PUBLIC,
                true,
                false,
                signature.getJavaReturnType(),
                signature.getJavaArgumentTypes(),
                Collections.emptyList()
        );

        CodeEmitter codeEmitter = methodEmitter.code();

        // Purposefully not using `thisLocal` here - we emit a static function that may very well
        // not have `this` at position 0.
        JavaLocal thisLocal = methodEmitter.getArgumentLocals().get(signature.getOwnerArgumentIndex());
        List<JavaLocal> staticLocals = new ArrayList<>();

        for (int argIndex = 0; argIndex < signature.getJavaArgumentTypes().size(); argIndex++) {
            if (argIndex == signature.getOwnerArgumentIndex()) {
                continue;
            }

            JavaLocal argumentLocal = methodEmitter.getArgumentLocals().get(signature.getArgumentIndex(argIndex));
            staticLocals.add(argumentLocal);
        }

        // Expand WASM locals
        List<ValueType> expandedLocals = new ArrayList<>();
        for (Local local : locals.asFlatArray()) {
            if (expandedLocals.size() + local.getCount() > 65535) {
                throw new WasmAssemblerException(
                        "Function has too many locals, maximum is 65535"
                );
            }

            for (int localIndex = 0; localIndex < local.getCount(); localIndex++) {
                expandedLocals.add(local.getType());

                JavaType javaType = WasmTypeConverter.toJavaType(local.getType());
                JavaLocal javaLocal = codeEmitter.allocateLocal(javaType);
                staticLocals.add(javaLocal);

                codeEmitter.loadConstant(javaType.getDefaultValue());
                codeEmitter.storeLocal(javaLocal);
            }
        }

        LocalVariables localVariables = new LocalVariables(thisLocal, staticLocals);

        WasmFrameState frameState = new WasmFrameState(
                Arrays.asList(functionType.getInputs().asFlatArray()),
                expandedLocals,
                signature.getWasmReturnTypes(),
                null
        );

        CodeEmitContext codeEmitContext = new CodeEmitContext(
                context.getLookups(),
                codeEmitter,
                frameState,
                context.getGenerators(),
                localVariables
        );

        for (InstructionInstance instruction : function.getExpr().getInstructions()) {
            this.processInstruction(instruction, codeEmitContext);
        }

        this.processFunctionEpilogue(codeEmitContext);

        // Finish code generation
        codeEmitter.finish();
        methodEmitter.finish();
    }

    private void processInstruction(
            InstructionInstance instruction,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        WasmInstruction<? extends WasmInstruction.Data> wasmInstruction = instruction.getInstruction();
        WasmInstruction.Data data = instruction.getData();

        try {
            wasmInstruction.emitCode(context, ObjectUtil.forceCast(data));
        } catch (WasmAssemblerException e) {
            throw new WasmAssemblerException("Could not process instruction " + wasmInstruction.getName(), e);
        }
    }

    private void processFunctionEpilogue(
            CodeEmitContext context
    ) throws WasmAssemblerException {
        List<ValueType> wasmOutputs = context.getFrameState().getReturnTypes();
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter codeEmitter = context.getEmitter();

        if (!frameState.isReachable()) {
            return;
        }

        // We have no trailing return instruction, insert one!
        if (wasmOutputs.size() > 1) {
            throw new WasmAssemblerException("Only one output is supported for now");
        } else if (wasmOutputs.isEmpty()) {
            codeEmitter.doReturn();
            return;
        }

        for (ValueType output : wasmOutputs) {
            frameState.popOperand(output);
        }

        codeEmitter.doReturn();
    }

    @Override
    public void emitStaticFunctionTableInitializer(CodeEmitContext context) throws WasmAssemblerException {
        context.getEmitter().invoke(
                context.getEmitter().getOwner(),
                "$initModuleFunctions",
                new JavaType[0],
                PrimitiveType.VOID,
                InvokeType.STATIC,
                false
        );
    }

    @Override
    public void emitFunctionTableInitializer(int functionCount, int firstLocalFunction, CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        emitter.loadConstant(functionCount);
        emitter.doNew(LINKED_FUNCTION_ARRAY_TYPE);

        emitter.duplicate();
        emitter.loadLocal(context.getLocalVariables().getThis());
        emitter.op(Op.SWAP);
        emitter.accessField(emitter.getOwner(), "functions", LINKED_FUNCTION_ARRAY_TYPE, false, true);

    }

    @Override
    public void emitLoadFunctionTable(CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();
        emitter.loadLocal(context.getLocalVariables().getThis());
        emitter.accessField(emitter.getOwner(), "functions", LINKED_FUNCTION_ARRAY_TYPE, false, false);
    }

    private String determineMethodName(LargeArrayIndex i) {
        return "$code_" + i;
    }

    private String determineFunctionTableInitializerName(int methodIndex) {
        return "$initModuleFunctions_" + methodIndex;
    }

    private FunctionType determineFunctionType(
            LargeArrayIndex i,
            ElementLookups lookups
    ) throws WasmAssemblerException {
        int functionTypeIndex = lookups.requireFunctionTypeIndex(i);
        return lookups.requireType(LargeArrayIndex.fromU32(functionTypeIndex));
    }
}
