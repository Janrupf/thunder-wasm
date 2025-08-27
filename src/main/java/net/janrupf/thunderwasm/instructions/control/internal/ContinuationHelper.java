package net.janrupf.thunderwasm.instructions.control.internal;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.continuation.ContinuationContext;
import net.janrupf.thunderwasm.assembler.continuation.ContinuationCutPoint;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaFrameSnapshot;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.runtime.continuation.Continuation;
import net.janrupf.thunderwasm.runtime.continuation.ContinuationLayer;
import net.janrupf.thunderwasm.types.ValueType;

import java.util.*;

public final class ContinuationHelper {
    public static final ObjectType CONTINUATION_TYPE = ObjectType.of(Continuation.class);
    public static final ObjectType CONTINUATION_LAYER_TYPE = ObjectType.of(ContinuationLayer.class);
    private static final ObjectType ILLEGAL_STATE_EXCEPTION_TYPE = ObjectType.of(IllegalStateException.class);

    /**
     * Emit the entry point for a function which uses continuations.
     *
     * @param context the context to use
     * @throws WasmAssemblerException if the code could not be emitted
     */
    public static void emitContinuationFunctionEntry(
            CodeEmitContext context
    ) throws WasmAssemblerException {
        JavaLocal continuationLocal = context.getLocalVariables().getContinuationLocal();
        CodeEmitter emitter = context.getEmitter();

        if (continuationLocal == null) {
            return;
        }

        emitter.loadLocal(continuationLocal);
        emitter.invoke(
                CONTINUATION_TYPE,
                "popLayerStatic",
                new JavaType[]{CONTINUATION_TYPE},
                CONTINUATION_LAYER_TYPE,
                InvokeType.STATIC,
                false
        );
        emitter.duplicate();
        emitter.jump(JumpCondition.IS_NOT_NULL, context.getContinuationContext().getRestoreBeginLabel(emitter));
        emitter.pop();
    }

    /**
     * Prepare a continuation point around a function call.
     * <p>
     * The argument types are used to compute a list of dummy values to push onto
     * the stack for repeating the function call for a continuation.
     *
     * @param context        the context to use
     * @param argumentTypes  the argument types of the function
     * @param extraSaveTypes additional values to save from the stack before the function arguments are discarded
     * @param javaReturnType the java return type of the invoked function
     * @return the id of the created point
     * @throws WasmAssemblerException if the code could not be emitted
     */
    public static ContinuationContext.PointAndLabel emitFunctionContinuationPoint(
            CodeEmitContext context,
            List<ValueType> argumentTypes,
            List<ValueType> extraSaveTypes,
            JavaType javaReturnType
    ) throws WasmAssemblerException {
        JavaLocal continuationLocal = context.getLocalVariables().getContinuationLocal();
        CodeEmitter emitter = context.getEmitter();

        if (continuationLocal == null) {
            return null;
        }

        ContinuationContext continuationContext = context.getContinuationContext();

        CodeLabel resumeLabel = emitter.newLabel();

        // Save the extra values
        emitter.resolveLabel(resumeLabel);

        List<JavaLocal> extraSaveLocals = new ArrayList<>(extraSaveTypes.size());
        for (int i = extraSaveTypes.size() - 1; i >= 0; i--) {
            JavaType javaExtraSaveType = WasmTypeConverter.toJavaType(extraSaveTypes.get(i));
            JavaLocal saveLocal = emitter.allocateLocal(javaExtraSaveType);

            extraSaveLocals.add(saveLocal);
            emitter.storeLocal(saveLocal);
        }

        // And load them back, effectively duplicating the values
        for (JavaLocal local : extraSaveLocals) {
            emitter.loadLocal(local);
        }

        return continuationContext.addPoint(
                emitter,
                resumeLabel,

                // Need to discard the returned value from the function
                javaReturnType.equals(PrimitiveType.VOID) ? Collections.emptyList() : Collections.singletonList(javaReturnType),
                getCompleteOperandStack(context),
                new ArrayList<>(argumentTypes),
                extraSaveTypes,
                extraSaveLocals
        );
    }

    /**
     * Emit the code for pausing a function post call if required.
     *
     * @param context       the context to use
     * @param pointAndLabel the point and label
     * @throws WasmAssemblerException if the code could not be emitted
     */
    public static void emitFunctionContinuationPointPostReturn(
            CodeEmitContext context,
            ContinuationContext.PointAndLabel pointAndLabel
    ) throws WasmAssemblerException {
        JavaLocal continuationLocal = context.getLocalVariables().getContinuationLocal();
        CodeEmitter emitter = context.getEmitter();

        if (continuationLocal == null) {
            return;
        }

        emitCheckIsPaused(context);

        ContinuationCutPoint point = pointAndLabel.getPoint();

        if (point.getExtraSaveLocals().isEmpty()) {
            // No need to restore any saved locals to the stack, jump to the pause point directly
            emitter.jump(JumpCondition.INT_NOT_EQUAL_ZERO, pointAndLabel.getPauseLabel());
        } else {
            CodeLabel noPauseLabel = emitter.newLabel();

            emitter.jump(JumpCondition.INT_EQUAL_ZERO, noPauseLabel);
            JavaFrameSnapshot snapshot = emitter.getStackFrameState().computeSnapshot();

            List<JavaLocal> savedLocals = point.getExtraSaveLocals();
            for (int i = savedLocals.size() - 1; i >= 0; i--) {
                JavaLocal saved = savedLocals.get(i);
                emitter.loadLocal(saved);
                saved.free();
            }

            emitter.jump(JumpCondition.ALWAYS, pointAndLabel.getPauseLabel());

            snapshot.dropInvalidatedLocals();
            noPauseLabel.overrideFrameSnapshot(snapshot);
            emitter.resolveLabel(noPauseLabel);
        }
    }

    /**
     * Emit the code required to check whether to pause based on the current continuation setting.
     *
     * @param context the context to use
     * @throws WasmAssemblerException if the code could not be emitted
     */
    private static void emitCheckIsPaused(
            CodeEmitContext context
    ) throws WasmAssemblerException {
        JavaLocal continuationLocal = context.getLocalVariables().getContinuationLocal();
        CodeEmitter emitter = context.getEmitter();

        if (continuationLocal == null) {
            return;
        }

        emitter.loadLocal(continuationLocal);
        emitter.invoke(
                CONTINUATION_TYPE,
                "isPaused",
                new JavaType[]{CONTINUATION_TYPE},
                PrimitiveType.BOOLEAN,
                InvokeType.STATIC,
                false
        );
    }

    /**
     * Emit the code for a continuation point.
     *
     * @param context the context to use
     * @throws WasmAssemblerException if the code could not be emitted
     */
    public static void emitContinuationPoint(
            CodeEmitContext context
    ) throws WasmAssemblerException {
        JavaLocal continuationLocal = context.getLocalVariables().getContinuationLocal();
        CodeEmitter emitter = context.getEmitter();

        if (continuationLocal == null) {
            return;
        }

        emitCheckShouldPause(context);

        ContinuationContext continuationContext = context.getContinuationContext();

        CodeLabel resumeLabel = emitter.newLabel();
        CodeLabel pauseLabel = continuationContext.addPoint(
                emitter,
                resumeLabel,
                Collections.emptyList(),
                getCompleteOperandStack(context),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        ).getPauseLabel();

        emitter.jump(JumpCondition.INT_NOT_EQUAL_ZERO, pauseLabel);
        emitter.resolveLabel(resumeLabel);
    }

    /**
     * Emit the code required to check whether to pause by polling the continuation
     * for a pause command.
     *
     * @param context the context to use
     * @throws WasmAssemblerException if the code could not be emitted
     */
    private static void emitCheckShouldPause(
            CodeEmitContext context
    ) throws WasmAssemblerException {
        JavaLocal continuationLocal = context.getLocalVariables().getContinuationLocal();
        CodeEmitter emitter = context.getEmitter();

        if (continuationLocal == null) {
            return;
        }

        emitter.loadLocal(continuationLocal);
        emitter.invoke(
                CONTINUATION_TYPE,
                "shouldPause",
                new JavaType[]{CONTINUATION_TYPE},
                PrimitiveType.BOOLEAN,
                InvokeType.STATIC,
                false
        );
    }

    /**
     * Emit the accumulated implementations for the continuation handlers.
     *
     * @param context            the context to use
     * @param functionReturnType the java return type of the function
     * @throws WasmAssemblerException if the code could not be emitted
     */
    public static void emitContinuationImplementations(
            CodeEmitContext context,
            JavaType functionReturnType
    ) throws WasmAssemblerException {
        JavaLocal continuationLocal = context.getLocalVariables().getContinuationLocal();
        if (continuationLocal == null) {
            return;
        }

        ContinuationContext continuationContext = context.getContinuationContext();
        CodeEmitter emitter = context.getEmitter();

        Map<ContinuationCutPoint, CodeLabel> cutPoints = new IdentityHashMap<>();
        Map<Integer, CodeLabel> initialJumpPoints = new HashMap<>();
        for (ContinuationCutPoint point : continuationContext.cutPoints()) {
            CodeLabel cutPointRestoreLabel = emitter.newLabel();
            cutPoints.put(point, cutPointRestoreLabel);

            for (int id : point.getResumeLabels().keySet()) {
                initialJumpPoints.put(id, cutPointRestoreLabel);
            }
        }

        CodeLabel unknownRestorePointLabel = emitter.newLabel();

        // Restore starts here, we expect the continuation layer to already be on top of the stack
        emitter.resolveLabel(continuationContext.getRestoreBeginLabel(emitter));

        Map<Integer, JavaLocal> localsById = context.getLocalVariables().getLocalsById();
        List<JavaLocal> locals = new ArrayList<>(localsById.values());
        locals.sort(Comparator.comparing(JavaLocal::getSlot));

        if (!localsById.isEmpty()) {
            emitter.duplicate();
            emitter.invoke(
                    CONTINUATION_LAYER_TYPE,
                    "getLocals",
                    new JavaType[0],
                    MultiValueHelper.MULTI_VALUE_TYPE,
                    InvokeType.VIRTUAL,
                    false
            );

            MultiValueHelper.emitRestoreLocals(emitter, locals, false);
        }

        JavaLocal heapLocals = context.getLocalVariables().getHeapLocals();
        if (heapLocals != null) {
            emitter.duplicate();
            emitter.invoke(
                    CONTINUATION_LAYER_TYPE,
                    "getHeapLocals",
                    new JavaType[0],
                    MultiValueHelper.MULTI_VALUE_TYPE,
                    InvokeType.VIRTUAL,
                    false
            );
            emitter.storeLocal(heapLocals);
        }

        emitter.duplicate();
        emitter.invoke(
                CONTINUATION_LAYER_TYPE,
                "getPointId",
                new JavaType[0],
                PrimitiveType.INT,
                InvokeType.VIRTUAL,
                false
        );

        emitter.lookupSwitch(unknownRestorePointLabel, initialJumpPoints);

        emitter.resolveLabel(unknownRestorePointLabel);
        emitFail(context, "Unknown top level restore point");

        // Emit code for individual restores
        for (Map.Entry<ContinuationCutPoint, CodeLabel> pointAndLabel : cutPoints.entrySet()) {
            ContinuationCutPoint point = pointAndLabel.getKey();

            emitter.resolveLabel(pointAndLabel.getValue());

            JavaLocal continuationLayerLocal = emitter.allocateLocal(CONTINUATION_LAYER_TYPE);
            emitter.duplicate();
            emitter.storeLocal(continuationLayerLocal);


            emitter.invoke(
                    CONTINUATION_LAYER_TYPE,
                    "getStack",
                    new JavaType[0],
                    MultiValueHelper.MULTI_VALUE_TYPE,
                    InvokeType.VIRTUAL,
                    false
            );

            MultiValueHelper.emitRestoreStack(
                    emitter,
                    ControlHelper.getJavaTypes(point.getOperandStack()),
                    null,
                    false
            );

            for (ValueType dummyType : point.getDummyOperands()) {
                JavaType javaDummyType = WasmTypeConverter.toJavaType(dummyType);

                Object defaultValue = javaDummyType.getDefaultValue();
                if (defaultValue == null) {
                    emitter.loadNull((ObjectType) javaDummyType);
                } else {
                    emitter.loadConstant(javaDummyType.getDefaultValue());
                }
            }

            if (!point.getExtraSaveTypes().isEmpty()) {
                emitter.loadLocal(continuationLayerLocal);
                emitter.invoke(
                        CONTINUATION_LAYER_TYPE,
                        "getStack",
                        new JavaType[0],
                        MultiValueHelper.MULTI_VALUE_TYPE,
                        InvokeType.VIRTUAL,
                        false
                );
                MultiValueHelper.emitRestoreStack(
                        emitter,
                        ControlHelper.getJavaTypes(point.getExtraSaveTypes()),
                        null,
                        false
                );
            }

            unknownRestorePointLabel = emitter.newLabel();

            emitter.loadLocal(continuationLayerLocal);
            emitter.invoke(
                    CONTINUATION_LAYER_TYPE,
                    "getPointId",
                    new JavaType[0],
                    PrimitiveType.INT,
                    InvokeType.VIRTUAL,
                    false
            );

            continuationLayerLocal.free();

            emitter.lookupSwitch(unknownRestorePointLabel, point.getResumeLabels());

            emitter.resolveLabel(unknownRestorePointLabel);
            emitFail(context, "Unknown specific restore point");
        }

        // And now the save implementations
        CodeLabel saveAndReturnLabel = emitter.newLabel();

        Iterator<ContinuationCutPoint> pointIterator = continuationContext.cutPoints().iterator();
        while (pointIterator.hasNext()) {
            ContinuationCutPoint point = pointIterator.next();
            CodeLabel saveLabel = emitter.newLabel();

            Iterator<Map.Entry<Integer, CodeLabel>> pauseLabels = point.getPauseLabels().entrySet().iterator();
            while (pauseLabels.hasNext()) {
                Map.Entry<Integer, CodeLabel> entry = pauseLabels.next();

                emitter.resolveLabel(entry.getValue());
                emitter.loadConstant(entry.getKey());
                if (pauseLabels.hasNext()) {
                    // If there are other labels, insert a jump, otherwise we can fall through
                    emitter.jump(JumpCondition.ALWAYS, saveLabel);
                }
            }

            if (saveLabel.isReachable()) {
                emitter.resolveLabel(saveLabel);
            }

            List<JavaType> extraSave = ControlHelper.getJavaTypes(point.getExtraSaveTypes());
            List<JavaType> stackOperands = ControlHelper.getJavaTypes(point.getOperandStack());

            List<JavaType> allSaveTypes = new ArrayList<>();
            allSaveTypes.addAll(extraSave);
            allSaveTypes.addAll(stackOperands);

            JavaLocal pointIdLocal = emitter.allocateLocal(PrimitiveType.INT);
            emitter.storeLocal(pointIdLocal);

            JavaLocal stackSaveLocal = emitter.allocateLocal(MultiValueHelper.MULTI_VALUE_TYPE);

            MultiValueHelper.emitCreateMultiValue(emitter, allSaveTypes);

            if (!extraSave.isEmpty()) {
                emitter.duplicate();
            }

            // TODO: We could save a few instructions here if we only store when
            //       discard count > 0 and keep the multi value on the stack if
            //       discard count < 1 after saving the extra save values
            emitter.storeLocal(stackSaveLocal);

            if (!extraSave.isEmpty()) {
                MultiValueHelper.emitSaveStack(emitter, extraSave, false);
            }

            for (int i = 0; i < point.getDiscardTypes().size(); i++) {
                emitter.pop();
            }

            emitter.loadLocal(stackSaveLocal);
            MultiValueHelper.emitSaveStack(emitter, stackOperands, true);

            emitter.loadLocal(pointIdLocal);

            stackSaveLocal.free();
            pointIdLocal.free();

            if (pointIterator.hasNext()) {
                emitter.jump(JumpCondition.ALWAYS, saveAndReturnLabel);
            }
        }

        if (continuationContext.cutPoints().isEmpty()) {
            // Nothing to save
            return;
        }

        if (saveAndReturnLabel.isReachable()) {
            emitter.resolveLabel(saveAndReturnLabel);
        }

        if (heapLocals != null) {
            emitter.loadLocal(heapLocals);
        } else {
            emitter.loadNull(MultiValueHelper.MULTI_VALUE_TYPE);
        }

        if (!locals.isEmpty()) {
            List<JavaType> localTypes = new ArrayList<>();
            for (JavaLocal local : locals) {
                localTypes.add(local.getType());
            }

            MultiValueHelper.emitCreateMultiValue(emitter, localTypes);
            MultiValueHelper.emitSaveLocals(emitter, locals, true);
        } else {
            emitter.loadNull(MultiValueHelper.MULTI_VALUE_TYPE);
        }

        emitter.invoke(
                CONTINUATION_LAYER_TYPE,
                "create",
                new JavaType[]{
                        MultiValueHelper.MULTI_VALUE_TYPE,
                        PrimitiveType.INT,
                        MultiValueHelper.MULTI_VALUE_TYPE,
                        MultiValueHelper.MULTI_VALUE_TYPE
                },
                CONTINUATION_LAYER_TYPE,
                InvokeType.STATIC,
                false
        );

        // Push the continuation layer
        emitter.loadLocal(continuationLocal);
        emitter.invoke(
                CONTINUATION_TYPE,
                "pushLayerStatic",
                new JavaType[]{CONTINUATION_LAYER_TYPE, CONTINUATION_TYPE},
                PrimitiveType.VOID,
                InvokeType.STATIC,
                false
        );

        // Load some dummy return value onto the stack
        if (!functionReturnType.equals(PrimitiveType.VOID)) {
            Object defaultValue = functionReturnType.getDefaultValue();
            if (defaultValue == null) {
                emitter.loadNull((ObjectType) functionReturnType);
            } else {
                emitter.loadConstant(defaultValue);
            }
        }

        emitter.doReturn();
    }

    /**
     * Emit code throwing an illegal state exception.
     *
     * @param context the context to use
     * @param message the message of the exception
     * @throws WasmAssemblerException if code could not be emitted
     */
    private static void emitFail(
            CodeEmitContext context,
            String message
    ) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        emitter.doNew(ILLEGAL_STATE_EXCEPTION_TYPE);
        emitter.duplicate();
        emitter.loadConstant(message);
        emitter.invoke(
                ILLEGAL_STATE_EXCEPTION_TYPE,
                "<init>",
                new JavaType[]{ObjectType.of(String.class)},
                PrimitiveType.VOID,
                InvokeType.SPECIAL,
                false
        );
        emitter.op(Op.THROW);
    }

    private static List<ValueType> getCompleteOperandStack(CodeEmitContext context) {
        List<ValueType> completeStack = new ArrayList<>();

        for (WasmFrameState frameState : context.getAllFrameStates()) {
            completeStack.addAll(frameState.getOperandStack());
        }

        return completeStack;
    }
}
