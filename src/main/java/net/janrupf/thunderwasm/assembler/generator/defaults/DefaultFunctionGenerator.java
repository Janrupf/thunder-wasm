package net.janrupf.thunderwasm.assembler.generator.defaults;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmPushedLabel;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.*;
import net.janrupf.thunderwasm.assembler.generator.FunctionGenerator;
import net.janrupf.thunderwasm.assembler.part.TranslatedFunctionSignature;
import net.janrupf.thunderwasm.imports.TableImportDescription;
import net.janrupf.thunderwasm.instructions.Function;
import net.janrupf.thunderwasm.instructions.InstructionInstance;
import net.janrupf.thunderwasm.instructions.Local;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.lookup.ElementLookups;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.runtime.WasmDynamicDispatch;
import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.TableType;
import net.janrupf.thunderwasm.types.ValueType;
import net.janrupf.thunderwasm.util.ObjectUtil;

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

        TranslatedFunctionSignature signature = TranslatedFunctionSignature.of(functionType, classEmitter.getOwner());
        LargeArray<Local> locals = function.getLocals();

        MethodEmitter methodEmitter = classEmitter.method(
                determineMethodName(i),
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

        CodeLabel returnLabel = codeEmitter.newLabel();
        WasmPushedLabel topLevelLabel = new WasmPushedLabel(returnLabel, functionType.getOutputs());
        codeEmitContext.pushBlock(frameState, topLevelLabel);

        for (InstructionInstance instruction : function.getExpr().getInstructions()) {
            this.processInstruction(instruction, codeEmitContext);
        }

        codeEmitContext.popBlock();

        if (returnLabel.isReachable()) {
            // Only resolve if its reachable either way
            codeEmitter.resolveLabel(returnLabel);
            frameState.markReachable();
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

        emitter.invoke(
                SIMPLE_LINKED_FUNCTION_TYPE,
                "inferFromMethodHandle",
                new JavaType[] { ObjectType.of(MethodHandle.class) },
                SIMPLE_LINKED_FUNCTION_TYPE,
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
