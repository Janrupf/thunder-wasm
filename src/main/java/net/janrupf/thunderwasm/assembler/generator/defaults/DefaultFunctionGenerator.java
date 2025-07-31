package net.janrupf.thunderwasm.assembler.generator.defaults;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmPushedLabel;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.analysis.AnalysisContext;
import net.janrupf.thunderwasm.assembler.analysis.AnalysisResult;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.*;
import net.janrupf.thunderwasm.assembler.generator.FunctionGenerator;
import net.janrupf.thunderwasm.assembler.part.TranslatedFunctionSignature;
import net.janrupf.thunderwasm.imports.TableImportDescription;
import net.janrupf.thunderwasm.instructions.Function;
import net.janrupf.thunderwasm.instructions.Local;
import net.janrupf.thunderwasm.instructions.control.internal.ControlHelper;
import net.janrupf.thunderwasm.instructions.control.internal.MultiValueHelper;
import net.janrupf.thunderwasm.lookup.ElementLookups;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.runtime.WasmDynamicDispatch;
import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.TableType;
import net.janrupf.thunderwasm.types.ValueType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DefaultFunctionGenerator implements FunctionGenerator {
    private static final ObjectType LINKED_FUNCTION_TYPE = ObjectType.of(LinkedFunction.class);
    private static final ObjectType SIMPLE_LINKED_FUNCTION_TYPE = ObjectType.of(LinkedFunction.Simple.class);
    private static final ObjectType DYNAMIC_DISPATCH_HELPER_TYPE = ObjectType.of(WasmDynamicDispatch.class);
    private static final ObjectType METHOD_HANDLE_TYPE = ObjectType.of(MethodHandle.class);
    private static final ObjectType METHOD_HANDLES_HELPER_TYPE = ObjectType.of(MethodHandles.class);

    @Override
    public void addFunction(LargeArrayIndex i, Function function, ClassEmitContext context) throws WasmAssemblerException {
        // Look up the function type
        FunctionType functionType = determineFunctionType(i, context.getLookups());
        ClassFileEmitter classEmitter = context.getEmitter();

        // Run code analysis
        AnalysisContext analysisContext = AnalysisContext.createForFunction(function.getExpr());
        analysisContext.run();

        AnalysisResult analysisResult = AnalysisResult.compileFromContext(analysisContext);

        TranslatedFunctionSignature signature = TranslatedFunctionSignature.of(functionType, classEmitter.getOwner());
        LargeArray<Local> locals = function.getLocals();

        String methodName = determineMethodName(i);
        MethodEmitter methodEmitter = classEmitter.method(
                methodName,
                Visibility.PRIVATE,
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
        List<JavaLocal> argumentLocals = new ArrayList<>();

        int expandedLocalCount = 0;
        for (Local local : locals.asFlatArray()) {
            expandedLocalCount += local.getCount();
        }

        boolean useHeapLocals = expandedLocalCount + signature.getJavaArgumentTypes().size() > 128;
        JavaLocal heapLocal = null;
        if (useHeapLocals) {
            heapLocal = codeEmitter.allocateLocal(MultiValueHelper.MULTI_VALUE_TYPE);
        }

        LocalVariables localVariables = new LocalVariables(thisLocal, heapLocal);

        for (int argIndex = 0; argIndex < signature.getJavaArgumentTypes().size(); argIndex++) {
            if (argIndex == signature.getOwnerArgumentIndex()) {
                continue;
            }

            JavaLocal argumentLocal = methodEmitter.getArgumentLocals().get(signature.getArgumentIndex(argIndex));
            argumentLocals.add(argumentLocal);

            localVariables.registerKnownLocal(signature.getArgumentIndex(argIndex), argumentLocal);
        }


        // Expand WASM locals
        List<ValueType> expandedLocals = new ArrayList<>();
        MultiValueHelper.IndexedBuilder heapLocalBuilder = useHeapLocals ? MultiValueHelper.indexedBuilder() : null;

        int localId = argumentLocals.size();
        for (Local local : locals.asFlatArray()) {
            if (expandedLocals.size() + local.getCount() > 65535) {
                throw new WasmAssemblerException(
                        "Function has too many locals, maximum is 65535"
                );
            }

            JavaType javaType = WasmTypeConverter.toJavaType(local.getType());

            if (useHeapLocals) {
                for (int localIndex = 0; localIndex < local.getCount(); localIndex++) {
                    expandedLocals.add(local.getType());
                    int heapIndex = heapLocalBuilder.allocate(javaType);

                    localVariables.registerKnownHeapLocal(localId++, javaType, heapIndex);
                }
            } else {
                codeEmitter.loadConstant(javaType.getDefaultValue());

                for (int localIndex = 0; localIndex < local.getCount(); localIndex++) {
                    expandedLocals.add(local.getType());

                    JavaLocal javaLocal = codeEmitter.allocateLocal(javaType);
                    localVariables.registerKnownLocal(localId++, javaLocal);

                    if (localIndex + 1 != local.getCount()) {
                        codeEmitter.duplicate();
                    }

                    codeEmitter.storeLocal(javaLocal);
                }
            }
        }

        if (useHeapLocals) {
            heapLocalBuilder.emitCreate(codeEmitter);
            codeEmitter.storeLocal(heapLocal);
        }

        WasmFrameState frameState = new WasmFrameState(
                Arrays.asList(functionType.getInputs().asFlatArray()),
                expandedLocals,
                signature.getWasmReturnTypes(),
                signature.getWasmReturnTypes()
        );

        CodeLabel returnLabel = codeEmitter.newLabel();
        WasmPushedLabel topLevelLabel = new WasmPushedLabel(returnLabel, functionType.getOutputs(), false);

        CodeEmitContext codeEmitContext = new CodeEmitContext(
                methodName + "$block$",
                analysisResult,
                classEmitter,
                codeEmitter,
                context.getLookups(),
                frameState,
                Collections.singletonList(topLevelLabel),
                context.getGenerators(),
                localVariables
        );

        ControlHelper.emitExpression(codeEmitContext, function.getExpr());

        if (returnLabel.isReachable()) {
            // Only resolve if its reachable either way
            codeEmitter.resolveLabel(returnLabel);
            codeEmitContext.getFrameState().markReachable();
        }

        this.processFunctionEpilogue(codeEmitContext);

        // Finish code generation
        codeEmitter.finish();
        methodEmitter.finish();
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

        for (int i = wasmOutputs.size() - 1; i >= 0; i--) {
            frameState.popOperand(wasmOutputs.get(i));
        }

        if (!frameState.getOperandStack().isEmpty()) {
            throw new WasmAssemblerException("Expected stack to be empty at implicit return, found " + frameState.getOperandStack().size() + " values");
        }

        if (!wasmOutputs.isEmpty() && wasmOutputs.size() > 1) {
            List<JavaType> javaTypes = Arrays.asList(WasmTypeConverter.toJavaTypes(wasmOutputs.toArray(new ValueType[0])));
            // Package up the return values into a MultiValue
            MultiValueHelper.emitCreateMultiValue(codeEmitter, javaTypes);
            MultiValueHelper.emitSaveStack(
                    codeEmitter,
                    javaTypes,
                    true
            );
        }

        codeEmitter.doReturn();
    }

    @Override
    public void emitInvokeFunction(LargeArrayIndex functionIndex, FunctionType function, CodeEmitContext context)
            throws WasmAssemblerException {
        TranslatedFunctionSignature signature = TranslatedFunctionSignature.of(function, context.getEmitter().getOwner());
        CodeEmitter emitter = context.getEmitter();

        emitter.loadLocal(context.getLocalVariables().getThis());
        emitter.invoke(
                emitter.getOwner(),
                determineMethodName(functionIndex),
                signature.getJavaArgumentTypes().toArray(new JavaType[0]),
                signature.getJavaReturnType(),
                InvokeType.STATIC,
                false
        );

        if (function.getOutputs().length() > 1) {
            MultiValueHelper.emitRestoreStack(
                    emitter,
                    Arrays.asList(WasmTypeConverter.toJavaTypes(function.getOutputs().asFlatArray())),
                    null,
                    false
            );
        }
    }

    @Override
    public void emitInvokeFunctionIndirect(
            FunctionType functionType,
            LargeArrayIndex tableIndex,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();
        FoundElement<TableType, TableImportDescription> element = context.getLookups().requireTable(tableIndex);

        if (element.isImport()) {
            context.getGenerators().getImportGenerator().emitTableGet(
                    element.getImport(),
                    context
            );
        } else {
            context.getGenerators().getTableGenerator().emitTableGet(
                    element.getIndex(),
                    element.getElement(),
                    context
            );
        }

        emitter.invoke(
                DYNAMIC_DISPATCH_HELPER_TYPE,
                "prepareCallIndirect",
                new JavaType[] { LINKED_FUNCTION_TYPE },
                METHOD_HANDLE_TYPE,
                InvokeType.STATIC,
                false
        );

        emitInvokeFunctionByMethodHandle(functionType, context);
    }

    public void emitInvokeFunctionByMethodHandle(FunctionType functionType, CodeEmitContext context)
            throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        // Use owner null here - we don't know the owner at this point, and prepareCallIndirect
        // will have bound the method handle to the correct owner if there is one.
        TranslatedFunctionSignature signature = TranslatedFunctionSignature.of(functionType, null);

        JavaLocal methodHandleLocal = emitter.allocateLocal(METHOD_HANDLE_TYPE);
        emitter.storeLocal(methodHandleLocal);
        CommonBytecodeGenerator.loadBelow(emitter, signature.getJavaArgumentTypes().size(), METHOD_HANDLE_TYPE,
                () -> emitter.loadLocal(methodHandleLocal));
        methodHandleLocal.free();

        emitter.invoke(
                METHOD_HANDLE_TYPE,
                "invokeExact",
                signature.getJavaArgumentTypes().toArray(new JavaType[0]),
                signature.getJavaReturnType(),
                InvokeType.VIRTUAL,
                false
        );

        if (functionType.getOutputs().length() > 1) {
            MultiValueHelper.emitRestoreStack(
                    emitter,
                    Arrays.asList(WasmTypeConverter.toJavaTypes(functionType.getOutputs().asFlatArray())),
                    null,
                    false
            );
        }
    }

    @Override
    public void emitLoadFunctionReference(LargeArrayIndex i, FunctionType functionType, CodeEmitContext context) throws WasmAssemblerException {
        emitLoadLinkedFunction(i, functionType, context);
    }

    @Override
    public void makeFunctionExportable(LargeArrayIndex i, FunctionType type, ClassEmitContext context) {
        // No-op, functions are exportable through loading their references linked implementations
    }

    @Override
    public void emitLoadFunctionExport(LargeArrayIndex i, FunctionType type, CodeEmitContext context)
            throws WasmAssemblerException {
        emitLoadLinkedFunction(i, type, context);
    }

    protected void emitLoadLinkedFunction(LargeArrayIndex i, FunctionType type, CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        TranslatedFunctionSignature signature = TranslatedFunctionSignature.of(type, emitter.getOwner());
        JavaMethodHandle handle = new JavaMethodHandle(
                emitter.getOwner(),
                determineMethodName(i),
                signature.getJavaReturnType(),
                signature.getJavaArgumentTypes(),
                InvokeType.STATIC,
                false
        );

        emitter.loadConstant(handle);

        if (signature.getOwnerArgumentIndex() != -1) {
            // Bind the handle to this
            emitter.loadConstant(signature.getOwnerArgumentIndex());
            emitter.loadConstant(1);
            emitter.doNew(new ArrayType(ObjectType.OBJECT));
            emitter.duplicate();
            emitter.loadConstant(0);
            emitter.loadLocal(context.getLocalVariables().getThis());
            emitter.storeArrayElement();
            emitter.invoke(
                    METHOD_HANDLES_HELPER_TYPE,
                    "insertArguments",
                    new JavaType[] { METHOD_HANDLE_TYPE, PrimitiveType.INT, new ArrayType(ObjectType.OBJECT) },
                    METHOD_HANDLE_TYPE,
                    InvokeType.STATIC,
                    false
            );
        }

        if (type.getOutputs().length() <= 1) {
            // Can use automatic inference
            emitter.invoke(
                    SIMPLE_LINKED_FUNCTION_TYPE,
                    "inferFromMethodHandle",
                    new JavaType[] { ObjectType.of(MethodHandle.class) },
                    SIMPLE_LINKED_FUNCTION_TYPE,
                    InvokeType.STATIC,
                    false
            );
        } else {
            // Manually pass the types
            emitter.doNew(SIMPLE_LINKED_FUNCTION_TYPE);
            emitter.duplicate(1, 1);
            emitter.op(Op.SWAP);
            loadValueTypeList(context, type.getInputs());
            loadValueTypeList(context, type.getOutputs());
            emitter.invoke(
                    SIMPLE_LINKED_FUNCTION_TYPE,
                    "<init>",
                    new JavaType[] { METHOD_HANDLE_TYPE, ObjectType.of(List.class), ObjectType.of(List.class) },
                    PrimitiveType.VOID,
                    InvokeType.SPECIAL,
                    false
            );
        }
    }

    private void loadValueTypeList(
            CodeEmitContext context,
            LargeArray<ValueType> types
    ) throws WasmAssemblerException {
        ValueType[] flat = types.asFlatArray();
        if (flat == null) {
            throw new WasmAssemblerException("Too many value types to load");
        }

        CodeEmitter emitter = context.getEmitter();
        emitter.loadConstant(flat.length);
        emitter.doNew(new ArrayType(ObjectType.of(ValueType.class)));

        for (int i = 0; i < flat.length; i++) {
            emitter.duplicate();
            emitter.loadConstant(i);
            CommonBytecodeGenerator.loadTypeReference(emitter, flat[i]);
            emitter.storeArrayElement();
        }

        emitter.invoke(
                ObjectType.of(Arrays.class),
                "asList",
                new JavaType[] { new ArrayType(ObjectType.OBJECT) },
                ObjectType.of(List.class),
                InvokeType.STATIC,
                false
        );
    }

    protected String determineMethodName(LargeArrayIndex i) {
        return "$code_" + i;
    }

    protected final FunctionType determineFunctionType(
            LargeArrayIndex i,
            ElementLookups lookups
    ) throws WasmAssemblerException {
        int functionTypeIndex = lookups.requireLocalFunctionTypeIndex(i);
        return lookups.requireType(LargeArrayIndex.fromU32(functionTypeIndex));
    }
}
