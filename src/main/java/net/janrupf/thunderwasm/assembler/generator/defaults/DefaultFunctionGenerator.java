package net.janrupf.thunderwasm.assembler.generator.defaults;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.generator.FunctionGenerator;
import net.janrupf.thunderwasm.assembler.part.TranslatedFunctionSignature;
import net.janrupf.thunderwasm.instructions.Function;
import net.janrupf.thunderwasm.instructions.InstructionInstance;
import net.janrupf.thunderwasm.instructions.Local;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.ValueType;
import net.janrupf.thunderwasm.util.ObjectUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DefaultFunctionGenerator implements FunctionGenerator {
    @Override
    public void addFunction(LargeArrayIndex i, Function function, ClassEmitContext context) throws WasmAssemblerException {
        // Look up the function type
        int functionTypeIndex = context.getLookups().requireFunctionTypeIndex(i);
        FunctionType functionType = context.getLookups().requireType(LargeArrayIndex.fromU32(functionTypeIndex));

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

    private String determineMethodName(LargeArrayIndex i) {
        return "$code_" + i;
    }
}
