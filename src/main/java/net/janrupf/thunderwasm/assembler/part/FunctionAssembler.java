package net.janrupf.thunderwasm.assembler.part;

import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.lookup.ElementLookups;
import net.janrupf.thunderwasm.lookup.ModuleLookups;
import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.instructions.Expr;
import net.janrupf.thunderwasm.instructions.InstructionInstance;
import net.janrupf.thunderwasm.instructions.Local;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.types.ValueType;
import net.janrupf.thunderwasm.util.ObjectUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper class to assemble a function to java bytecode.
 */
public final class FunctionAssembler {
    private final ModuleLookups lookups;
    private final WasmGenerators generators;
    private final LargeArray<Local> locals;
    private final Expr expr;

    public FunctionAssembler(
            ModuleLookups lookups,
            WasmGenerators generators,
            LargeArray<Local> locals,
            Expr expr
    ) {
        this.lookups = lookups;
        this.generators = generators;
        this.locals = locals;
        this.expr = expr;
    }

    public void assemble(
            ClassFileEmitter classEmitter,
            String functionName,
            LargeArray<ValueType> inputs,
            LargeArray<ValueType> outputs
    ) throws WasmAssemblerException  {
        try {
            TranslatedFunctionSignature signature = TranslatedFunctionSignature.of(inputs, outputs, classEmitter.getOwner());

            MethodEmitter methodEmitter = classEmitter.method(
                    functionName,
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

            for (int i = 0; i < signature.getJavaArgumentTypes().size(); i++) {
                if (i == signature.getOwnerArgumentIndex()) {
                    continue;
                }

                JavaLocal argumentLocal = methodEmitter.getArgumentLocals().get(signature.getArgumentIndex(i));
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

                for (int i = 0; i < local.getCount(); i++) {
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
                    inputs.asFlatArray(),
                    expandedLocals,
                    signature.getWasmReturnTypes(),
                    null
            );

            CodeEmitContext context = new CodeEmitContext(
                    new ElementLookups(lookups),
                    codeEmitter,
                    frameState,
                    generators,
                    localVariables
            );

            for (InstructionInstance instruction : expr.getInstructions()) {
                this.processInstruction(instruction, context);
            }

            this.processFunctionEpilogue(context);

            // Finish code generation
            codeEmitter.finish();
            methodEmitter.finish();
        } catch (WasmAssemblerException e) {
            throw new WasmAssemblerException("Failed to generate function " + functionName, e);
        }
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
}
