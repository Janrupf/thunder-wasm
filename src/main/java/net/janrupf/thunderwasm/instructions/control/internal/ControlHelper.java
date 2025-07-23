package net.janrupf.thunderwasm.instructions.control.internal;

import net.janrupf.thunderwasm.assembler.*;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.CodeLabel;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
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
    public static void emitPushBlock(CodeEmitContext context, BlockType blockType) throws WasmAssemblerException {
        emitPushBlock(context, blockType, false);
    }

    /**
     * Emit the required sequence to push a block.
     *
     * @param context      the context to use
     * @param blockType    the block type to push
     * @param resolveLabel whether to resolve the block jump label to the start of the block
     * @throws WasmAssemblerException if the block can not be pushed
     */
    public static void emitPushBlock(CodeEmitContext context, BlockType blockType, boolean resolveLabel)
            throws WasmAssemblerException {
        FunctionType functionType = expandBlockType(context, blockType);
        CodeLabel label = context.getEmitter().newLabel();

        WasmPushedLabel pushedLabel;
        if (resolveLabel) {
            context.getEmitter().resolveLabel(label);
            pushedLabel = new WasmPushedLabel(label, functionType.getInputs());
        } else {
            pushedLabel = new WasmPushedLabel(label, functionType.getOutputs());
        }

        WasmFrameState newFrameState = context.getFrameState().executeBlock(functionType);
        context.pushBlock(newFrameState, pushedLabel);
    }

    /**
     * Emit the required sequence to pop a block.
     *
     * @param context the context to use
     * @throws WasmAssemblerException if the block can not be popped
     */
    public static void emitPopBlock(CodeEmitContext context) throws WasmAssemblerException {
        emitPopBlock(context, false);
    }

    /**
     * Emit the required sequence to pop a block.
     *
     * @param context      the context to use
     * @param resolveLabel whether to resolve the block jump label to the end of the block
     * @throws WasmAssemblerException if the block can not be popped
     */
    public static void emitPopBlock(CodeEmitContext context, boolean resolveLabel) throws WasmAssemblerException {
        if (context.getFrameState().isReachable()) {
            emitBlockExit(context);
        }

        WasmPushedLabel blockJumpLabel = context.getBlockJumpLabel();
        if (blockJumpLabel == null) {
            throw new WasmAssemblerException("Attempted to emit code for popping a block outside of a block");
        }

        context.popBlock();

        JavaFrameSnapshot fixed = null;
        if (context.getEmitter().getStackFrameState() == null) {
            // This probably requires a bit of explanation...
            //
            // If we get here the emitter has lost track of what the state of the frame
            // currently is. This happens when the WASM code is unreachable after a block
            // as the emitter has no reference what the current Java stack frame should
            // look like (after all, nothing jumped there, so the state may be whatever).
            //
            // We need to fix this situation by taking the WASM state and transforming it
            // back into Java state. Easier said than done - the assembler may decide to
            // inject, re-order and otherwise mangle with the locals and operands. So
            // we infer a "pure" view of Java state from the WASM state ("pure" as in
            // doesn't account for emitter specific changes) and then ask the assembler
            // to re-do its changes on the stack frame.
            //
            // TODO: This wont work with an unreachable block inside another block
            //       because the frame state only takes into account the most inner frame
            JavaFrameSnapshot inferred = context.getFrameState().inferJavaFrameSnapshot();
            fixed = context.fixupInferredFrameSnapshot(inferred);
        }

        if (resolveLabel) {
            context.getFrameState().markReachable();
            context.getEmitter().resolveLabel(blockJumpLabel.getCodeLabel(), fixed);
        } else if(fixed != null) { // See comment from above for why we inject a label here
            CodeLabel fixupLabel = context.getEmitter().newLabel();
            context.getEmitter().resolveLabel(fixupLabel, fixed);
        }
    }

    /**
     * Emit the instructions required to clean the stack before jumping to a label.
     *
     * @param context the context to use
     * @param depth how many labels to go up
     * @throws WasmAssemblerException if the instructions can not be emitted
     * @return the label to jump to
     */
    public static CodeLabel emitCleanStackForBlockLabel(CodeEmitContext context, int depth) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();
        WasmPushedLabel blockJumpLabel = context.getBlockJumpLabel(depth);

        if (blockJumpLabel == null) {
            throw new WasmAssemblerException("Not enough labels on the stack to pop " + depth + " labels");
        }

        List<ValueType> operandStack = frameState.getOperandStack();
        LargeArray<ValueType> blockReturnTypes = blockJumpLabel.getStackOperands();

        int returnCount = (int) blockReturnTypes.length();
        int discardCount = operandStack.size() - returnCount;

        if (discardCount < 1) {
            // No need to emit java instructions, the stack is already in the correct state
            // (or underflown, but then popOperand below will throw)
            for (ValueType returnType : blockReturnTypes) {
                frameState.popOperand(returnType);
            }

            return blockJumpLabel.getCodeLabel();
        } else if (discardCount == 1 && returnCount == 1) {
            // Check if we can go a fast path where we only need to swap and then drop the top value

            JavaType javaReturnType = WasmTypeConverter.toJavaType(blockReturnTypes.get(LargeArrayIndex.ZERO));
            JavaType javaDiscardType = WasmTypeConverter.toJavaType(operandStack.get(operandStack.size() - 2));
            if (javaReturnType.getSlotCount() < 2 && javaDiscardType.getSlotCount() < 2) {
                // Single slot value which needs to be discarded, swap and drop
                frameState.popOperand(blockReturnTypes.get(LargeArrayIndex.ZERO));

                emitter.op(Op.SWAP);
                emitter.pop();
                return blockJumpLabel.getCodeLabel();
            }
        }

        // (maybe) slow path: Back up the values from the top of the stack into locals, discard, and push back
        JavaLocal[] locals = new JavaLocal[returnCount];
        for (int i = returnCount - 1; i >= 0; i--) {
            locals[i] = emitter.allocateLocal(WasmTypeConverter.toJavaType(blockReturnTypes.get(LargeArrayIndex.ZERO.add(i))));
            emitter.storeLocal(locals[i]);
        }

        for (int i = 0; i < discardCount; i++) {
            emitter.pop();
        }

        // Restore stack
        for (JavaLocal local : locals) {
            emitter.loadLocal(local);
            local.free();
        }

        return blockJumpLabel.getCodeLabel();
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
