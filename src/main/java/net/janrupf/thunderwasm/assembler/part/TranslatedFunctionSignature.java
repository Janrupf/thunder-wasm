package net.janrupf.thunderwasm.assembler.part;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.instructions.control.internal.ContinuationHelper;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.runtime.state.MultiValue;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.ValueType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class TranslatedFunctionSignature {
    private final List<ValueType> wasmArgumentTypes;
    private final List<JavaType> javaArgumentTypes;
    private final List<ValueType> wasmReturnTypes;
    private final JavaType javaReturnType;
    private final int ownerArgumentIndex;
    private final int continuationArgumentIndex;

    private TranslatedFunctionSignature(
            List<ValueType> wasmArgumentTypes,
            List<JavaType> javaArgumentTypes,
            List<ValueType> wasmReturnTypes,
            JavaType javaReturnType,
            int ownerArgumentIndex,
            int continuationArgumentIndex
    ) {
        this.wasmArgumentTypes = wasmArgumentTypes;
        this.javaArgumentTypes = javaArgumentTypes;
        this.wasmReturnTypes = wasmReturnTypes;
        this.javaReturnType = javaReturnType;
        this.ownerArgumentIndex = ownerArgumentIndex;
        this.continuationArgumentIndex = continuationArgumentIndex;
    }

    /**
     * Retrieves the types of the arguments of the function in WebAssembly format.
     *
     * @return the list of argument types in WebAssembly format
     */
    public List<ValueType> getWasmArgumentTypes() {
        return wasmArgumentTypes;
    }

    /**
     * Retrieves the types of the arguments of the function.
     *
     * @return the list of argument types
     */
    public List<JavaType> getJavaArgumentTypes() {
        return javaArgumentTypes;
    }

    /**
     * Retrieves the return types of the function in WebAssembly format.
     *
     * @return the list of return types in WebAssembly format
     */
    public List<ValueType> getWasmReturnTypes() {
        return wasmReturnTypes;
    }

    /**
     * Retrieves the return type of the function.
     *
     * @return the return type
     */
    public JavaType getJavaReturnType() {
        return javaReturnType;
    }

    /**
     * Retrieves the index of the owner argument in the Java function signature.
     *
     * @return the index of the owner argument, or -1, if there is no owner argument
     */
    public int getOwnerArgumentIndex() {
        return ownerArgumentIndex;
    }

    /**
     * Retrieves the index of the continuation argument in the Java function signature.
     *
     * @return the index of the continuation argument, or -1, if there is no continuation argument
     */
    public int getContinuationArgumentIndex() {
        return continuationArgumentIndex;
    }

    /**
     * Retrieves the index of the given argument in the Java function signature.
     *
     * @param argument the index of the argument to retrieve
     * @return the index of the argument in the Java function signature
     */
    public int getArgumentIndex(int argument) {
        if (ownerArgumentIndex != -1 && argument >= ownerArgumentIndex) {
            argument++;
        }

        if (continuationArgumentIndex != -1 && argument >= continuationArgumentIndex) {
            argument++;
        }

        return argument;
    }

    /**
     * Compute the translated function signature from the given WebAssembly function type.
     *
     * @param functionType       the WebAssembly function type to translate
     * @param owner              the type that owns the function
     * @param enableContinuation whether the function has continuations enabled
     * @return the translated function signature
     * @throws WasmAssemblerException if the translation fails
     */
    public static TranslatedFunctionSignature of(
            FunctionType functionType,
            ObjectType owner,
            boolean enableContinuation
    ) throws WasmAssemblerException {
        return of(functionType.getInputs(), functionType.getOutputs(), owner, enableContinuation);
    }

    /**
     * Compute the translated function signature from the given inputs and outputs.
     *
     * @param inputs             the inputs of the function
     * @param outputs            the outputs of the function
     * @param owner              the type that owns the function
     * @param enableContinuation whether the function has continuations enabled
     * @return the translated function signature
     * @throws WasmAssemblerException if the translation fails
     */
    public static TranslatedFunctionSignature of(
            LargeArray<ValueType> inputs,
            LargeArray<ValueType> outputs,
            ObjectType owner,
            boolean enableContinuation
    ) throws WasmAssemblerException {
        if (Long.compareUnsigned(inputs.length(), 255) > 0) {
            throw new WasmAssemblerException(
                    "Function has too many inputs, maximum is 255"
            );
        }

        List<JavaType> javaArgumentTypes = new ArrayList<>(Arrays.asList(WasmTypeConverter.toJavaTypes(inputs.asFlatArray())));

        // The owner (this) becomes the last argument
        int ownerArgumentIndex = -1;
        if (owner != null) {
            ownerArgumentIndex = javaArgumentTypes.size();
            javaArgumentTypes.add(owner);
        }

        int continuationArgumentIndex = -1;
        if (enableContinuation) {
            continuationArgumentIndex = javaArgumentTypes.size();
            javaArgumentTypes.add(ContinuationHelper.CONTINUATION_TYPE);
        }

        JavaType returnType;
        switch ((int) outputs.length()) {
            case 0:
                // No outputs
                returnType = PrimitiveType.VOID;
                break;
            case 1:
                // 1 output
                returnType = WasmTypeConverter.toJavaType(outputs.get(LargeArrayIndex.ZERO));
                break;
            default:
                // Multiple outputs
                returnType = ObjectType.of(MultiValue.class);
                break;
        }

        List<ValueType> wasmArgumentTypes = Arrays.asList(inputs.asFlatArray());
        List<ValueType> wasmReturnTypes = Arrays.asList(outputs.asFlatArray());

        return new TranslatedFunctionSignature(
                wasmArgumentTypes,
                javaArgumentTypes,
                wasmReturnTypes,
                returnType,
                ownerArgumentIndex,
                continuationArgumentIndex
        );
    }
}
