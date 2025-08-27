package net.janrupf.thunderwasm.instructions.control.internal;

import net.janrupf.thunderwasm.assembler.*;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.instructions.Expr;
import net.janrupf.thunderwasm.instructions.InstructionInstance;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.BlockType;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.ValueType;

import java.util.Arrays;
import java.util.List;

public final class ControlHelper {
    private ControlHelper() {
        throw new AssertionError("Control helper is a singleton and cannot be instantiated");
    }

    /**
     * Expand a {@link BlockType} into its corresponding {@link FunctionType}.
     *
     * @param context   the context to use
     * @param blockType the block type to expand
     * @return the block type expanded as a function type
     * @throws WasmAssemblerException if the block type can not be expanded
     */
    public static FunctionType expandBlockType(CodeEmitContext context, BlockType blockType)
            throws WasmAssemblerException {
        if (blockType instanceof BlockType.Empty) {
            return new FunctionType(LargeArray.of(ValueType.class), LargeArray.of(ValueType.class));
        } else if (blockType instanceof BlockType.Value) {
            return new FunctionType(
                    LargeArray.of(ValueType.class),
                    LargeArray.of(ValueType.class, ((BlockType.Value) blockType).getValueType())
            );
        } else if (blockType instanceof BlockType.TypeIndex) {
            LargeArrayIndex i = LargeArrayIndex.fromU32(((BlockType.TypeIndex) blockType).getTypeIndex());
            return context.getLookups().requireType(i);
        } else {
            throw new WasmAssemblerException("Unknown block type " + blockType);
        }
    }

    /**
     * Emit an expression.
     *
     * @param context the context to use
     * @param expr    the expression to emit
     * @throws WasmAssemblerException if the expression fails to be emitted
     */
    public static void emitExpression(CodeEmitContext context, Expr expr) throws WasmAssemblerException {
        for (InstructionInstance instructionInstance : expr.getInstructions()) {
            emitInstruction(context, instructionInstance.getInstruction(), instructionInstance.getData());
        }
    }

    @SuppressWarnings("unchecked")
    private static <D extends WasmInstruction.Data> void emitInstruction(
            CodeEmitContext context,
            WasmInstruction<D> instruction,
            Object instructionData
    ) throws WasmAssemblerException {
        D data = (D) instructionData;

        try {
            ProcessedInstruction processedInstruction = instruction.processInputs(context, data);

            if (context.getFrameState().isReachable()) {
                processedInstruction.emitBytecode(context);
            } else {
                processedInstruction.processUnreachable(context);
            }

            processedInstruction.processOutputs(context);
        } catch (WasmAssemblerException e) {
            throw new WasmAssemblerException(
                    "Could not process instruction " + instruction.getName() + " inside control block", e);
        }
    }

    public static void popArguments(CodeEmitContext context, FunctionType functionType) throws WasmAssemblerException {
        LargeArrayIndex i = functionType.getInputs().largeLength();
        while (i.compareTo(LargeArrayIndex.ZERO) > 0) {
            i = i.subtract(1);
            context.getFrameState().popOperand(functionType.getInputs().get(i));
        }
    }

    public static void pushReturnValues(CodeEmitContext context, FunctionType functionType) throws WasmAssemblerException {
        for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(functionType.getOutputs().largeLength()) < 0; i = i.add(1)) {
            context.getFrameState().pushOperand(functionType.getOutputs().get(i));
        }
    }

    public static List<JavaType> getJavaReturnTypes(FunctionType type) throws WasmAssemblerException {
        return getJavaTypes(type.getOutputs());
    }

    public static List<JavaType> getJavaTypes(LargeArray<ValueType> wasmTypes) throws WasmAssemblerException {
        ValueType[] flatWasmTypes = wasmTypes.asFlatArray();
        if (flatWasmTypes == null) {
            throw new WasmAssemblerException("Too many types");
        }

        return Arrays.asList(WasmTypeConverter.toJavaTypes(flatWasmTypes));
    }

    public static List<JavaType> getJavaTypes(List<ValueType> wasmTypes) throws WasmAssemblerException {
        return Arrays.asList(WasmTypeConverter.toJavaTypes(wasmTypes.toArray(new ValueType[0])));
    }
}
