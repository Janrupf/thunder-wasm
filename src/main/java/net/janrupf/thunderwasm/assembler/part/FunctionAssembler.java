package net.janrupf.thunderwasm.assembler.part;

import net.janrupf.thunderwasm.lookup.ElementLookups;
import net.janrupf.thunderwasm.lookup.ModuleLookups;
import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.instructions.Expr;
import net.janrupf.thunderwasm.instructions.InstructionInstance;
import net.janrupf.thunderwasm.instructions.Local;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.ValueType;

import java.util.ArrayList;
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
            LargeArray<ValueType> outputs,
            boolean isStatic
    ) throws WasmAssemblerException  {
        try {
            if (Long.compareUnsigned(inputs.length(), 255) > 0) {
                throw new WasmAssemblerException(
                        "Function has too many inputs, maximum is 255"
                );
            }

            // Somewhat arbitrary limit, but we need to limit the number of outputs...
            // besides that, if you need more than 255 outputs, you're doing something wrong
            if (Long.compareUnsigned(outputs.length(), 255) > 0) {
                throw new WasmAssemblerException(
                        "Function has too many outputs, maximum is 255"
                );
            }

            if (Long.compareUnsigned(outputs.length(), 1) > 0) {
                throw new WasmAssemblerException(
                        "Function has too many outputs, currently only 1 is supported"
                );
            }

            ValueType wasmReturnType;
            JavaType returnType;
            if (outputs.length() != 0) {
                // 1 output
                wasmReturnType = outputs.get(LargeArrayIndex.ZERO);
                returnType = WasmTypeConverter.toJavaType(wasmReturnType);
            } else {
                // 0 outputs
                wasmReturnType = null;
                returnType = PrimitiveType.VOID;
            }

            MethodEmitter methodEmitter = classEmitter.method(
                    functionName,
                    // TODO: Exports...
                    Visibility.PUBLIC,
                    isStatic,
                    false,
                    returnType,
                    WasmTypeConverter.toJavaTypes(inputs.asFlatArray()),
                    new JavaType[0]
            );

            CodeEmitter codeEmitter = methodEmitter.code();

            // Expand locals
            List<ValueType> expandedLocals = new ArrayList<>();
            for (Local local : locals.asFlatArray()) {
                if (expandedLocals.size() + local.getCount() > 65535) {
                    throw new WasmAssemblerException(
                            "Function has too many locals, maximum is 65535"
                    );
                }

                for (int i = 0; i < local.getCount(); i++) {
                    expandedLocals.add(local.getType());
                }
            }

            WasmFrameState frameState = new WasmFrameState(
                    inputs.asFlatArray(),
                    expandedLocals,
                    wasmReturnType,
                    null
            );

            for (InstructionInstance instruction : expr.getInstructions()) {
                this.processInstruction(instruction, codeEmitter, frameState);
            }

            this.processFunctionEpilogue(codeEmitter, frameState, outputs, returnType);

            // Finish code generation
            codeEmitter.finish();
            methodEmitter.finish();
        } catch (WasmAssemblerException e) {
            throw new WasmAssemblerException("Failed to generate function " + functionName, e);
        }
    }

    private void processInstruction(
            InstructionInstance instruction,
            CodeEmitter codeEmitter,
            WasmFrameState frameState
    ) throws WasmAssemblerException {
        WasmInstruction<? extends WasmInstruction.Data> wasmInstruction = instruction.getInstruction();
        WasmInstruction.Data data = instruction.getData();

        // This intermediary helper method is necessary in order to be able to cast
        // the data to the correct type
        this.processInstructionWithData(wasmInstruction, data, codeEmitter, frameState);
    }

    @SuppressWarnings("unchecked")
    private <D extends WasmInstruction.Data> void processInstructionWithData(
            WasmInstruction<D> instruction,
            Object instructionData,
            CodeEmitter codeEmitter,
            WasmFrameState frameState
    ) throws WasmAssemblerException {
        D data = (D) instructionData;
        try {
            instruction.emitCode(
                    new CodeEmitContext(new ElementLookups(lookups), codeEmitter, frameState, generators),
                    data
            );
        } catch (WasmAssemblerException e) {
            throw new WasmAssemblerException("Could not process instruction " + instruction.getName(), e);
        }
    }

    private void processFunctionEpilogue(
            CodeEmitter codeEmitter,
            WasmFrameState frameState,
            LargeArray<ValueType> wasmOutputs,
            JavaType javaOutput
    ) throws WasmAssemblerException {
        if (!frameState.isReachable()) {
            return;
        }

        // We have no trailing return instruction, insert one!
        if (wasmOutputs.length() > 1) {
            throw new WasmAssemblerException("Only one output is supported for now");
        } else if (wasmOutputs.length() < 1) {
            codeEmitter.doReturn();
            return;
        }

        ValueType output = wasmOutputs.get(LargeArrayIndex.ZERO);
        JavaType javaOutputType = WasmTypeConverter.toJavaType(output);

        // We expect the output to be on the stack

        if (!javaOutputType.equals(javaOutput)) {
            throw new WasmAssemblerException(
                    "Expected output type " + javaOutput.toJvmDescriptor() + " but got " + javaOutputType.toJvmDescriptor()
            );
        }

        codeEmitter.doReturn();
    }
}
