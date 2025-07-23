package net.janrupf.thunderwasm.assembler.part;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
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

    private TranslatedFunctionSignature(
            List<ValueType> wasmArgumentTypes,
            List<JavaType> javaArgumentTypes,
            List<ValueType> wasmReturnTypes,
            JavaType javaReturnType,
            int ownerArgumentIndex
    ) {
        this.wasmArgumentTypes = wasmArgumentTypes;
        this.javaArgumentTypes = javaArgumentTypes;
        this.wasmReturnTypes = wasmReturnTypes;
        this.javaReturnType = javaReturnType;
        this.ownerArgumentIndex = ownerArgumentIndex;
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
     * @return the index of the owner argument
     */
    public int getOwnerArgumentIndex() {
        return ownerArgumentIndex;
    }

    /**
     * Retrieves the index of the given argument in the Java function signature.
     *
     * @param argument the index of the argument to retrieve
     * @return the index of the argument in the Java function signature
     */
    public int getArgumentIndex(int argument) {
        int remappedIndex = javaArgumentTypes.size() - 1 - argument;

        if (remappedIndex <= ownerArgumentIndex) {
            remappedIndex--;
        }

        return  remappedIndex;
    }

    /**
     * Compute the translated function signature from the given WebAssembly function type.
     *
     * @param functionType the WebAssembly function type to translate
     * @param owner the type that owns the function
     * @return the translated function signature
     * @throws WasmAssemblerException if the translation fails
     */
    public static TranslatedFunctionSignature of(FunctionType functionType, ObjectType owner) throws WasmAssemblerException {
        return of(functionType.getInputs(), functionType.getOutputs(), owner);
    }

    /**
     * Compute the translated function signature from the given inputs and outputs.
     *
     * @param inputs the inputs of the function
     * @param outputs the outputs of the function
     * @param owner the type that owns the function
     * @return the translated function signature
     * @throws WasmAssemblerException if the translation fails
     */
    public static TranslatedFunctionSignature of(
            LargeArray<ValueType> inputs,
            LargeArray<ValueType> outputs,
            ObjectType owner
    ) throws WasmAssemblerException  {
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

        // We reverse the java function signature because WASM pops arguments from the stack in reverse order
        JavaType[] originalJavaArgumentTypes = WasmTypeConverter.toJavaTypes(inputs.asFlatArray());

        List<JavaType> javaArgumentTypes = new ArrayList<>(originalJavaArgumentTypes.length + 1);
        for (int i = 0; i < originalJavaArgumentTypes.length; i++) {
            javaArgumentTypes.add(originalJavaArgumentTypes[originalJavaArgumentTypes.length - 1 - i]);
        }

        // ... And the owner (this) becomes the last argument
        javaArgumentTypes.add(owner);

        JavaType returnType;
        if (outputs.length() != 0) {
            // 1 output
            returnType = WasmTypeConverter.toJavaType(outputs.get(LargeArrayIndex.ZERO));
        } else {
            // 0 outputs
            returnType = PrimitiveType.VOID;
        }

        List<ValueType> wasmArgumentTypes = Arrays.asList(inputs.asFlatArray());
        List<ValueType> wasmReturnTypes = Arrays.asList(outputs.asFlatArray());

        return new TranslatedFunctionSignature(
                wasmArgumentTypes,
                javaArgumentTypes,
                wasmReturnTypes,
                returnType,
                javaArgumentTypes.size() - 1
        );
    }
}
