package net.janrupf.thunderwasm.instructions.control.internal;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmPushedLabel;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.analysis.LocalVariableUsage;
import net.janrupf.thunderwasm.assembler.continuation.ContinuationContext;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.instructions.Expr;
import net.janrupf.thunderwasm.instructions.control.BlockData;
import net.janrupf.thunderwasm.runtime.state.BlockReturn;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.ValueType;

import java.util.*;

public final class BlockHelper {
    public static final String BLOCK_RETURN_ENTRY_POINT = "block_return";
    private static final ObjectType BLOCK_RETURN_TYPE = ObjectType.of(BlockReturn.class);

    private BlockHelper() {
        throw new AssertionError("Static utility class");
    }

    /**
     * Emit the code required to invoke a block.
     *
     * @param context          the context to use
     * @param block            the block to invoke
     * @param primary          whether to use the primary or secondary expression
     * @param selfLabelAtStart whether the block's own label is at the start or end
     * @throws WasmAssemblerException if the code could not be emitted
     */
    public static void emitInvokeBlock(
            CodeEmitContext context,
            BlockData block,
            boolean primary,
            boolean selfLabelAtStart
    ) throws WasmAssemblerException {
        Expr expr = primary ? block.getPrimaryExpression() : block.getSecondaryExpression();

        if (context.getAnalysisResult().shouldSplitBlock(expr)) {
            emitInvokeSplitBlock(context, block, primary, selfLabelAtStart);
        } else {
            emitInvokeNonSplitBlock(context, block, primary, selfLabelAtStart);
        }
    }

    /**
     * Emit the code required to invoke a block inline.
     *
     * @param context          the context to use
     * @param block            the block to invoke
     * @param primary          whether to use the primary or secondary expression
     * @param selfLabelAtStart whether the block's own label is at the start or end
     * @throws WasmAssemblerException if the code could not be emitted
     */
    private static void emitInvokeNonSplitBlock(
            CodeEmitContext context,
            BlockData block,
            boolean primary,
            boolean selfLabelAtStart
    ) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();
        Expr expr = primary ? block.getPrimaryExpression() : block.getSecondaryExpression();

        FunctionType type = ControlHelper.expandBlockType(context, block.getType());

        CodeLabel blockSelfLabel = emitter.newLabel();

        WasmFrameState originalFrameState = context.getFrameState();
        context.pushBlock(originalFrameState.beginBlock(type), new WasmPushedLabel(
                blockSelfLabel,
                selfLabelAtStart ? type.getInputs() : type.getOutputs(),
                false
        ));

        if (selfLabelAtStart) {
            emitter.resolveLabel(blockSelfLabel);
            ContinuationHelper.emitContinuationPoint(context);
        }

        ControlHelper.emitExpression(context, expr);

        WasmFrameState blockFrameState = context.getFrameState();
        if (blockFrameState.isReachable()) {
            // This is not necessarily the same as the block end being reachable -
            // here we handle the fallthrough case where the block expression
            // reaches the end without a jump
            List<ValueType> flatOutputs = type.getOutputs().asFlatList();
            if (flatOutputs == null) {
                throw new WasmAssemblerException("Block has too many outputs");
            }

            for (int i = flatOutputs.size() - 1; i >= 0; i--) {
                blockFrameState.popOperand(flatOutputs.get(i));
            }

            if (!blockFrameState.getOperandStack().isEmpty()) {
                throw new WasmAssemblerException("Block expression left values on the stack, expected empty stack after block: "
                        + blockFrameState.getOperandStack());
            }
        }

        boolean blockEndReachable = blockFrameState.isReachable() || (!selfLabelAtStart && blockSelfLabel.isReachable());
        if (!selfLabelAtStart && blockEndReachable) {
            emitter.resolveLabel(blockSelfLabel);
        }

        if (blockEndReachable) {
            originalFrameState.markReachable();
        } else {
            originalFrameState.markUnreachable();
        }

        context.popBlock();
        originalFrameState.endBlock(type);
    }

    /**
     * Emit the code required to invoke a block as a split function.
     *
     * @param context          the context to use
     * @param block            the block to invoke
     * @param primary          whether to use the primary or secondary expression
     * @param selfLabelAtStart whether the block's own label is at the start or end
     * @throws WasmAssemblerException if the code could not be emitted
     */
    private static void emitInvokeSplitBlock(
            CodeEmitContext context,
            BlockData block,
            boolean primary,
            boolean selfLabelAtStart
    ) throws WasmAssemblerException {
        String blockName = context.nextBlockName();
        CodeEmitter localEmitter = context.getEmitter();
        Expr expr = primary ? block.getPrimaryExpression() : block.getSecondaryExpression();

        FunctionType type = ControlHelper.expandBlockType(context, block.getType());

        ValueType[] wasmInputs = type.getInputs().asFlatArray();
        if (wasmInputs == null) {
            throw new WasmAssemblerException("Block has too many inputs");
        }

        List<JavaType> javaInputs = new ArrayList<>(Arrays.asList(WasmTypeConverter.toJavaTypes(wasmInputs)));

        int localValuesRestoreIndex = javaInputs.size();
        javaInputs.add(MultiValueHelper.MULTI_VALUE_TYPE);

        JavaLocal thisLocal = context.getLocalVariables().getThis();

        int blockThisLocalIndex = -1;
        if (thisLocal != null) {
            blockThisLocalIndex = javaInputs.size();
            javaInputs.add(thisLocal.getType());
        }

        JavaLocal heapLocal = context.getLocalVariables().getHeapLocals();
        int blockHeapLocalsIndex = -1;
        if (heapLocal != null) {
            blockHeapLocalsIndex = javaInputs.size();
            javaInputs.add(heapLocal.getType());
        }

        JavaLocal continuationLocal = context.getLocalVariables().getContinuationLocal();
        int blockContinuationLocalIndex = -1;
        if (continuationLocal != null) {
            blockContinuationLocalIndex = javaInputs.size();
            javaInputs.add(ContinuationHelper.CONTINUATION_TYPE);
        }

        // Create a new method for the block
        MethodEmitter blockMethodEmitter = context.getClassFileEmitter().method(
                blockName,
                Visibility.PRIVATE,
                true,
                false,
                BLOCK_RETURN_TYPE,
                javaInputs,
                Collections.emptyList()
        );

        CodeEmitter blockCodeEmitter = blockMethodEmitter.code();

        // Create non-local labels for all existing labels
        List<WasmPushedLabel> nonLocalLabels = new ArrayList<>();
        for (WasmPushedLabel label : context.getAllBlockJumpLabels()) {
            nonLocalLabels.add(new WasmPushedLabel(
                    null,
                    label.getStackOperands(),
                    true
            ));
        }

        JavaLocal blockLocalsValueRestoreLocal = blockMethodEmitter.getArgumentLocals().get(localValuesRestoreIndex);

        JavaLocal blockThisLocal = null;
        if (thisLocal != null) {
            blockThisLocal = blockMethodEmitter.getArgumentLocals().get(blockThisLocalIndex);
        }

        JavaLocal blockHeapLocal = null;
        if (heapLocal != null) {
            blockHeapLocal = blockMethodEmitter.getArgumentLocals().get(blockHeapLocalsIndex);
        }

        JavaLocal blockContinuationLocal = null;
        if (continuationLocal != null) {
            blockContinuationLocal = blockMethodEmitter.getArgumentLocals().get(blockContinuationLocalIndex);
        }

        // Calculate the required transfer of local variables into the block and reverse
        LocalVariables blockLocalVariables = new LocalVariables(blockThisLocal, blockHeapLocal, blockContinuationLocal);

        List<JavaLocal> localSaveLocals = new ArrayList<>();
        List<JavaLocal> blockRestoreLocals = new ArrayList<>();
        List<JavaType> blockReadLocalTypes = new ArrayList<>();

        List<JavaLocal> blockSaveLocals = new ArrayList<>();
        List<JavaLocal> localRestoreLocals = new ArrayList<>();
        List<JavaType> blockWriteLocalTypes = new ArrayList<>();

        List<JavaLocal> writeOnlyLocals = new ArrayList<>();

        for (Map.Entry<Integer, LocalVariableUsage.Status> localUsageEntry :
                context.getAnalysisResult().getLocalVariableUsage(expr).getStatus().entrySet()) {
            int localId = localUsageEntry.getKey();

            if (context.getLocalVariables().getType(localId) == LocalVariables.LocalType.HEAP) {
                // Heap locals are passed through automatically since the entire heap local
                // storage is passed down
                LocalVariables.HeapLocal l = context.getLocalVariables().requireHeapById(localId);
                blockLocalVariables.registerKnownHeapLocal(localId, l.getType(), l.getIndex());
                continue;
            }

            LocalVariableUsage.Status usage = localUsageEntry.getValue();

            JavaLocal originalLocal = context.getLocalVariables().requireById(localId);
            JavaLocal blockLocal = blockCodeEmitter.allocateLocal(originalLocal.getType());
            blockLocalVariables.registerKnownLocal(localId, blockLocal);

            if (usage.wasRead()) {
                // Save locally, restore in block
                localSaveLocals.add(originalLocal);
                blockRestoreLocals.add(blockLocal);
                blockReadLocalTypes.add(originalLocal.getType());
            }

            if (usage.wasWritten()) {
                // Save in block, restore locally
                blockSaveLocals.add(blockLocal);
                localRestoreLocals.add(originalLocal);
                blockWriteLocalTypes.add(originalLocal.getType());
            }

            if (!usage.wasRead() && usage.wasWritten()) {
                writeOnlyLocals.add(blockLocal);
            }
        }

        // Emit the restore code inside the block

        // This is a bit annoying, but if the self label is at the start, we need to
        // initialize the local variables before we emit the expression, even if they
        // are just written. Otherwise the Verifier will complain about uninitialized locals.
        // TODO: This is a bit of a hack, we should probably have a better way to handle this
        //       (as in, emit correct stack map frames which track local usage).
        for (JavaLocal local : writeOnlyLocals) {
            blockCodeEmitter.loadConstant(local.getType().getDefaultValue());
            blockCodeEmitter.storeLocal(local);
        }

        blockCodeEmitter.loadLocal(blockLocalsValueRestoreLocal);
        MultiValueHelper.emitRestoreLocals(blockCodeEmitter, blockRestoreLocals, false);

        WasmFrameState blockFrameState = context.getFrameState().beginBlock(type);

        CodeEmitContext blockContext = new CodeEmitContext(
                blockName + "$",
                context.getAnalysisResult(),
                context.getClassFileEmitter(),
                blockCodeEmitter,
                context.getLookups(),
                null,
                nonLocalLabels,
                context.getGenerators(),
                blockLocalVariables,
                context.getConfiguration()
        );

        // TODO: This should probably be before the locals are initialized with zero values
        ContinuationHelper.emitContinuationFunctionEntry(blockContext);

        CodeLabel blockReturnLabel = blockCodeEmitter.newLabel();
        blockContext.getLocalGadgets().addEntryPoint(BLOCK_RETURN_ENTRY_POINT, blockReturnLabel);

        CodeLabel blockSelfLabel = blockCodeEmitter.newLabel();
        blockContext.pushBlock(blockFrameState, new WasmPushedLabel(
                blockSelfLabel,
                selfLabelAtStart ? type.getInputs() : type.getOutputs(),
                false
        ));

        // Restore the stack
        for (int i = 0; i < wasmInputs.length; i++) {
            blockCodeEmitter.loadLocal(blockMethodEmitter.getArgumentLocals().get(i));
        }

        if (selfLabelAtStart) {
            blockCodeEmitter.resolveLabel(blockSelfLabel);
        }

        ControlHelper.emitExpression(blockContext, expr);

        if (!selfLabelAtStart && blockSelfLabel.isReachable()) {
            blockCodeEmitter.resolveLabel(blockSelfLabel);
            blockContext.getFrameState().markReachable();
        }

        if (blockContext.getFrameState().isReachable()) {
            // Could potentially fall through, we need to capture the block return values

            List<JavaType> blockReturnTypes = ControlHelper.getJavaReturnTypes(type);

            MultiValueHelper.emitCreateMultiValue(blockCodeEmitter, blockReturnTypes);
            MultiValueHelper.emitSaveStack(blockCodeEmitter, blockReturnTypes, true);
            UnwindHelper.emitUnwindStack(blockCodeEmitter, 1);

            // -2 is the special value that indicates fallthrough
            blockCodeEmitter.loadConstant(-2);
            blockCodeEmitter.op(Op.SWAP);

            // The return entry point is appended directly to this function, we can simply fall through here
        }

        // Construct the block return if it was used
        if (blockReturnLabel.isReachable() || blockContext.getFrameState().isReachable()) {
            // At this point we expect on the stack:
            // - int: return depth
            // - multi value: block return values
            blockCodeEmitter.resolveLabel(blockReturnLabel);

            MultiValueHelper.emitCreateMultiValue(blockCodeEmitter, blockWriteLocalTypes);
            MultiValueHelper.emitSaveLocals(blockCodeEmitter, blockSaveLocals, true);

            blockCodeEmitter.invoke(
                    BLOCK_RETURN_TYPE,
                    "create",
                    new JavaType[]{PrimitiveType.INT, MultiValueHelper.MULTI_VALUE_TYPE, MultiValueHelper.MULTI_VALUE_TYPE},
                    BLOCK_RETURN_TYPE,
                    InvokeType.STATIC,
                    false
            );

            // And return it!
            blockCodeEmitter.doReturn();
        }

        ContinuationHelper.emitContinuationImplementations(blockContext, MultiValueHelper.MULTI_VALUE_TYPE);

        blockCodeEmitter.finish();
        blockMethodEmitter.finish();

        // On to generating the function call in the local block
        ContinuationContext.PointAndLabel pointAndLabel = ContinuationHelper.emitFunctionContinuationPoint(
                context,
                type.getInputs().asFlatList(),
                Collections.emptyList(),
                BLOCK_RETURN_TYPE
        );

        MultiValueHelper.emitCreateMultiValue(localEmitter, blockReadLocalTypes);
        MultiValueHelper.emitSaveLocals(localEmitter, localSaveLocals, true);

        if (thisLocal != null) {
            localEmitter.loadLocal(thisLocal);
        }

        if (heapLocal != null) {
            localEmitter.loadLocal(heapLocal);
        }

        if (continuationLocal != null) {
            localEmitter.loadLocal(continuationLocal);
        }

        localEmitter.invoke(
                blockCodeEmitter.getOwner(),
                blockName,
                javaInputs.toArray(new JavaType[0]),
                BLOCK_RETURN_TYPE,
                InvokeType.STATIC,
                false
        );

        ContinuationHelper.emitFunctionContinuationPointPostReturn(context, pointAndLabel);

        // We now need to decide how to operate based on the received depth
        Map<Integer, CodeLabel> switchTargets = new HashMap<>();
        for (int depth = 0; depth < nonLocalLabels.size(); depth++) {
            WasmPushedLabel nonLocalLabel = nonLocalLabels.get(nonLocalLabels.size() - 1 - depth);
            if (nonLocalLabel.isUsed()) {
                // This target label was used by the block, we need to handle its return
                switchTargets.put(depth, localEmitter.newLabel());
            }
        }

        if (context.getAnalysisResult().usesDirectReturn(expr)) {
            // Need to also handle the direct return
            switchTargets.put(-1, localEmitter.newLabel());
        }

        // Unpack the data the block returned
        localEmitter.duplicate();
        localEmitter.invoke(
                BLOCK_RETURN_TYPE,
                "getStack",
                new JavaType[0],
                MultiValueHelper.MULTI_VALUE_TYPE,
                InvokeType.VIRTUAL,
                false
        );
        localEmitter.op(Op.SWAP);

        if (!switchTargets.isEmpty()) {
            // We'll need it again later down
            localEmitter.duplicate();
        }

        localEmitter.invoke(
                BLOCK_RETURN_TYPE,
                "getLocals",
                new JavaType[0],
                MultiValueHelper.MULTI_VALUE_TYPE,
                InvokeType.VIRTUAL,
                false
        );

        // Restore locals
        MultiValueHelper.emitRestoreLocals(localEmitter, localRestoreLocals, false);

        if (!switchTargets.isEmpty() || context.getAnalysisResult().usesDirectReturn(expr)) {
            // Get return depth to the top of the stack
            localEmitter.invoke(
                    BLOCK_RETURN_TYPE,
                    "getBranchDepth",
                    new JavaType[0],
                    PrimitiveType.INT,
                    InvokeType.VIRTUAL,
                    false
            );

            CodeLabel fallthroughTarget = localEmitter.newLabel();

            localEmitter.lookupSwitch(fallthroughTarget, switchTargets);

            // Fallthrough case is now done, we need to generate the other cases
            for (Map.Entry<Integer, CodeLabel> returnHandlers : switchTargets.entrySet()) {
                int depth = returnHandlers.getKey();

                localEmitter.resolveLabel(returnHandlers.getValue());

                if (depth == -1) {
                    emitHandleNonLocalDirectReturn(context);
                } else {
                    emitHandleNonLocalBlockReturn(context, depth);
                }
            }

            // Fallthrough: we received an unknown value, this means fallthrough
            localEmitter.resolveLabel(fallthroughTarget);
        }

        context.getFrameState().endBlock(type);

        // TODO: What if the invoked block end itself was unreachable?
        context.getFrameState().markReachable();

        MultiValueHelper.emitRestoreStack(localEmitter, ControlHelper.getJavaReturnTypes(type), null, false);
    }

    /**
     * Emit the code that leaves a block and jumps to the given depth.
     *
     * @param context the context to use
     * @param depth   the depth to jump to
     * @throws WasmAssemblerException if the code could not be emitted
     */
    public static void emitBlockReturn(
            CodeEmitContext context,
            int depth
    ) throws WasmAssemblerException {
        WasmPushedLabel targetLabel = context.getBlockJumpLabel(depth);
        if (targetLabel == null) {
            throw new WasmAssemblerException("No label available at depth " + depth);
        }

        targetLabel.markUsed();

        List<JavaType> labelArity = ControlHelper.getJavaTypes(targetLabel.getStackOperands());

        if (targetLabel.isNonLocal()) {
            emitNonLocalBlockReturn(context, calculateNonLocalDepth(context, depth), labelArity);
        } else {
            emitLocalBlockReturn(context, depth, targetLabel.getCodeLabel(), labelArity);
        }

        context.getFrameState().markUnreachable();
    }

    /**
     * Emit the code that jumps to a local block return label.
     *
     * @param context    the context to use
     * @param label      the local block label
     * @param labelArity the stack operands expected at the label
     * @throws WasmAssemblerException if the code could not be emitted
     */
    private static void emitLocalBlockReturn(
            CodeEmitContext context,
            int depth,
            CodeLabel label,
            List<JavaType> labelArity
    ) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        // We potentially need to discard multiple layers of blocks from here - gather the
        // entire stack state that needs to be dropped
        List<ValueType> completeOperandStack = new ArrayList<>();
        for (int i = depth; i >= 0; i--) {
            completeOperandStack.addAll(context.getFrameState(i).getOperandStack());
        }

        int returnCount = labelArity.size();
        int discardCount = completeOperandStack.size() - returnCount;

        UnwindHelper.emitUnwindStack(emitter, labelArity.size(), discardCount);
        emitter.jump(JumpCondition.ALWAYS, label);
    }

    /**
     * Create the code that jumps to the local block return entry point.
     * <p>
     * The depth this function wants as an argument can be calculated as
     * n'th value of non-local labels from the right.
     *
     * @param context    the context to use
     * @param depth      the return depth to push
     * @param labelArity the stack operands expected at the label
     * @throws WasmAssemblerException if the code could not be emitted
     */
    private static void emitNonLocalBlockReturn(
            CodeEmitContext context,
            int depth,
            List<JavaType> labelArity
    ) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        MultiValueHelper.emitCreateMultiValue(emitter, labelArity);
        MultiValueHelper.emitSaveStack(emitter, labelArity, true);

        emitResumeNonLocalAfterReturn(context, depth, labelArity);
    }

    /**
     * Handle the non-local return from an inner block.
     * <p>
     * Assumes a multi value with the block return values is
     * on top of the stack.
     *
     * @param context the context to use
     * @param depth   the depth to handle
     * @throws WasmAssemblerException if the code could not be emitted
     */
    private static void emitHandleNonLocalBlockReturn(
            CodeEmitContext context,
            int depth
    ) throws WasmAssemblerException {
        WasmPushedLabel targetLabel = context.getBlockJumpLabel(depth);
        if (targetLabel == null) {
            throw new WasmAssemblerException("No label available at depth " + depth);
        }

        targetLabel.markUsed();

        List<JavaType> labelArity = ControlHelper.getJavaTypes(targetLabel.getStackOperands());

        if (targetLabel.isNonLocal()) {
            emitResumeNonLocalAfterReturn(context, calculateNonLocalDepth(context, depth), labelArity);
        } else {
            emitResumeLocalAfterReturn(context, depth, targetLabel.getCodeLabel(), labelArity);
        }
    }

    /**
     * Emit the code required to resume at a local label after a non-local return.
     *
     * @param context     the context to use
     * @param depth       the label depth to resume at
     * @param targetLabel the target label to resume at
     * @param labelArity  the operand stack at the label
     * @throws WasmAssemblerException if the code could not be emitted
     */
    private static void emitResumeLocalAfterReturn(
            CodeEmitContext context,
            int depth,
            CodeLabel targetLabel,
            List<JavaType> labelArity
    ) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        // We potentially need to discard multiple layers of blocks from here - gather the
        // entire stack state that needs to be dropped
        List<ValueType> completeOperandStack = new ArrayList<>();
        for (int i = depth; i >= 0; i--) {
            completeOperandStack.addAll(context.getFrameState(i).getOperandStack());
        }

        // We need to keep exactly 1 value: The multi return
        UnwindHelper.emitUnwindStack(emitter, 1, completeOperandStack.size());
        MultiValueHelper.emitRestoreStack(emitter, labelArity, null, false);

        // Jump after restoration
        emitter.jump(JumpCondition.ALWAYS, targetLabel);
    }

    /**
     * Emit the code required to resume at a non-local label after a non-local return.
     *
     * @param context    the context to use
     * @param depth      the return depth to push
     * @param labelArity the stack operands expected at the label
     * @throws WasmAssemblerException if the code could not be emitted
     */
    private static void emitResumeNonLocalAfterReturn(
            CodeEmitContext context,
            int depth,
            List<JavaType> labelArity
    ) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        // Discard everything we don't need from the stack now
        UnwindHelper.emitUnwindStack(emitter, 1);

        // Load the depth and make sure that the multi value is on top in the end
        emitter.loadConstant(depth);
        emitter.op(Op.SWAP);

        if (emitter.getStackFrameState().operandStackCount() != 2) {
            throw new WasmAssemblerException("Expected exactly two values on the stack, but found " + emitter.getStackFrameState().operandStackCount());
        }

        CodeLabel returnEntryPoint = context.getLocalGadgets().getEntryPoint(BLOCK_RETURN_ENTRY_POINT);
        if (returnEntryPoint == null) {
            throw new WasmAssemblerException("Tried to emit block return in a block which doesn't have non-local returns");
        }

        emitter.jump(JumpCondition.ALWAYS, returnEntryPoint);
    }

    /**
     * Emit the code required to perform a direct return.
     *
     * @param context the context to use
     */
    public static void emitDirectReturn(
            CodeEmitContext context
    ) throws WasmAssemblerException {
        List<ValueType> returnTypes = context.getFrameState().getReturnTypes();
        List<JavaType> javaReturnTypes = ControlHelper.getJavaTypes(returnTypes);
        CodeEmitter emitter = context.getEmitter();

        List<WasmPushedLabel> labels = context.getAllBlockJumpLabels();
        if (labels.isEmpty() || !labels.get(0).isNonLocal()) {
            // Top level method (ie. not a block), return directly
            if (returnTypes.size() > 1) {
                MultiValueHelper.emitCreateMultiValue(emitter, javaReturnTypes);
                MultiValueHelper.emitSaveStack(emitter, javaReturnTypes, true);
            }

            emitter.doReturn();
            context.getFrameState().markUnreachable();
            return;
        }

        MultiValueHelper.emitCreateMultiValue(emitter, javaReturnTypes);
        MultiValueHelper.emitSaveStack(emitter, javaReturnTypes, true);

        emitDirectReturnResumeUnwind(context);
    }

    /**
     * Handle a non-local direct return from a block.
     *
     * @param context the context to use
     * @throws WasmAssemblerException if the code could not be emitted
     */
    private static void emitHandleNonLocalDirectReturn(
            CodeEmitContext context
    ) throws WasmAssemblerException {
        // At this point a multi value is on top of the stack, either resume unwinding
        // or return here
        List<ValueType> returnTypes = context.getFrameState().getReturnTypes();
        List<JavaType> javaReturnTypes = ControlHelper.getJavaTypes(returnTypes);
        CodeEmitter emitter = context.getEmitter();

        List<WasmPushedLabel> labels = context.getAllBlockJumpLabels();
        if (labels.isEmpty() || !labels.get(0).isNonLocal()) {
            // Top level method (ie. not a block), return directly
            if (javaReturnTypes.size() == 1) {
                // Unpack the multi value
                MultiValueHelper.emitRestoreStack(emitter, javaReturnTypes, null, false);
            }

            emitter.doReturn();
            context.getFrameState().markUnreachable();
            return;
        }

        // Resume unwind
        emitDirectReturnResumeUnwind(context);
    }

    /**
     * Emit the code required to resume unwinding for a direct return.
     *
     * @param context the context to use
     * @throws WasmAssemblerException if the code could not be emitted
     */
    private static void emitDirectReturnResumeUnwind(
            CodeEmitContext context
    ) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        // Keep only the multi value on the stack, unwind everything else
        UnwindHelper.emitUnwindStack(emitter, 1);

        // -1 is a special constant to signal complete unwind and return
        emitter.loadConstant(-1);
        emitter.op(Op.SWAP);

        if (emitter.getStackFrameState().operandStackCount() != 2) {
            throw new WasmAssemblerException("Expected exactly two values on the stack, but found " + emitter.getStackFrameState().operandStackCount());
        }

        CodeLabel returnEntryPoint = context.getLocalGadgets().getEntryPoint(BLOCK_RETURN_ENTRY_POINT);
        if (returnEntryPoint == null) {
            // ???? If we are not in a block then where are we?
            throw new WasmAssemblerException("Tried to emit block return in a block which doesn't have non-local returns");
        }

        emitter.jump(JumpCondition.ALWAYS, returnEntryPoint);
        context.getFrameState().markUnreachable();
    }

    /**
     * Calculate the non-local return depth of the given block depth.
     *
     * @param context the context to use
     * @param depth   the non-local depth
     * @return the non-local depth
     * @throws WasmAssemblerException if the requested depth is a local label
     */
    private static int calculateNonLocalDepth(
            CodeEmitContext context,
            int depth
    ) throws WasmAssemblerException {
        WasmPushedLabel label = context.getBlockJumpLabel(depth);
        if (label == null || !label.isNonLocal()) {
            throw new WasmAssemblerException("Invalid non-local block depth " + depth);
        }

        List<WasmPushedLabel> allLabels = context.getAllBlockJumpLabels();

        int nonLocalDepth = 0;
        for (int i = allLabels.size() - depth; i < allLabels.size() && allLabels.get(i).isNonLocal(); i++) {
            nonLocalDepth++;
        }

        return nonLocalDepth;
    }
}
