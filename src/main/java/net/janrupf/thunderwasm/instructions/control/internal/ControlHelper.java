package net.janrupf.thunderwasm.instructions.control.internal;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeLabel;
import net.janrupf.thunderwasm.instructions.Expr;
import net.janrupf.thunderwasm.instructions.InstructionInstance;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.BlockType;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.ValueType;

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
     * Emit the required sequence to push a block.
     *
     * @param context   the context to use
     * @param blockType the block type to push
     * @throws WasmAssemblerException if the block can not be pushed
     */
    public static void emitPushBlock(CodeEmitContext context, BlockType blockType)
            throws WasmAssemblerException {
        FunctionType functionType = expandBlockType(context, blockType);
        CodeLabel label = context.getEmitter().newLabel();

        WasmFrameState newFrameState = context.getFrameState().executeBlock(functionType);
        context.pushBlock(newFrameState, label);
    }

    /**
     * Emit the required sequence to pop a block.
     *
     * @param context the context to use
     * @throws WasmAssemblerException if the block can not be popped
     */
    public static void emitPopBlock(CodeEmitContext context) throws WasmAssemblerException {
        if (context.getFrameState().isReachable()) {
            emitBlockExit(context);
        }

        CodeLabel blockEndLabel = context.getBlockEndLabel();
        context.popBlock();

        // TODO: This could fail if the block end is not reachable for some reason.
        //       The emitter will not know the frame state and subsequently not know
        //       how to resolve the label. This could probably be fixed by re-computing
        //       the Java frame state from the WASM frame stat
        context.getFrameState().markReachable();
        context.getEmitter().resolveLabel(blockEndLabel);
    }

    /**
     * Emit the required sequence for closing a block.
     * <p>
     * In other words, pop all the block return values, discard remaining stack
     * and restore return values.
     *
     * @param context the context to use
     * @throws WasmAssemblerException if the exit code can't be emitted
     */
    public static void emitBlockExit(CodeEmitContext context) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();

        List<ValueType> blockReturnTypes = frameState.getBlockReturnTypes();
        for (int i = blockReturnTypes.size() - 1; i >= 0; i--) {
            frameState.popOperand(blockReturnTypes.get(i));
        }

        if (!frameState.getOperandStack().isEmpty()) {
            throw new WasmAssemblerException(
                    "Expected empty stack on block exit, but got " + frameState.getOperandStack().size() +
                            " remaining items");
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
            instruction.emitCode(context, data);
        } catch (WasmAssemblerException e) {
            throw new WasmAssemblerException(
                    "Could not process instruction " + instruction.getName() + " inside control block", e);
        }
    }
}
