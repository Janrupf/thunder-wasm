package net.janrupf.thunderwasm.assembler.continuation;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.CodeLabel;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.types.ValueType;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ContinuationContext {
    private final Map<String, ContinuationCutPoint> cutPoints;
    private int nextContinuationId;
    private CodeLabel restoreBeginLabel;

    public ContinuationContext() {
        this.nextContinuationId = 0;
        this.cutPoints = new HashMap<>();
    }

    /**
     * Retrieve the label to jump to for beginning a restore.
     *
     * @param emitter the emitter to use
     * @return the label to jump to for beginning a restore
     */
    public CodeLabel getRestoreBeginLabel(CodeEmitter emitter) {
        if (this.restoreBeginLabel == null) {
            this.restoreBeginLabel = emitter.newLabel();
        }

        return this.restoreBeginLabel;
    }

    /**
     * Insert a new continuation point.
     *
     * @param emitter         the emitter to use
     * @param resumePoint     the point to jump to for resuming execution
     * @param discardTypes    the values to discard from the top of the stack
     * @param operandStack    the operand stack that needs to be saved
     * @param dummyOperands   the operands to push default values for
     * @param extraSaveTypes  additional values to save and restore after the dummy operands
     * @param extraSaveLocals the locals that hold the additionally saved values
     * @return the created point
     * @throws WasmAssemblerException if the data can not be computed
     */
    public PointAndLabel addPoint(
            CodeEmitter emitter,
            CodeLabel resumePoint,
            List<JavaType> discardTypes,
            List<ValueType> operandStack,
            List<ValueType> dummyOperands,
            List<ValueType> extraSaveTypes,
            List<JavaLocal> extraSaveLocals
    ) throws WasmAssemblerException {
        int id = this.nextContinuationId++;

        String key;

        if (!extraSaveLocals.isEmpty()) {
            // Can't unify points which require extra locals, we'll just
            // make unique key instead
            key = "unique:" + id;
        } else {
            key = continuationKey(discardTypes, operandStack, dummyOperands, extraSaveTypes);
        }

        ContinuationCutPoint point;

        if (!cutPoints.containsKey(key)) {
            point = new ContinuationCutPoint(discardTypes, operandStack, dummyOperands, extraSaveTypes, extraSaveLocals);
            cutPoints.put(key, point);
        } else {
            point = cutPoints.get(key);
        }

        CodeLabel pauseLabel = emitter.newLabel();
        point.addResumePoint(id, resumePoint, pauseLabel);
        return new PointAndLabel(point, pauseLabel);
    }

    /**
     * Retrieve all cut points of this context.
     *
     * @return all cut points
     */
    public Collection<ContinuationCutPoint> cutPoints() {
        return this.cutPoints.values();
    }

    private String continuationKey(
            List<JavaType> discardTypes,
            List<ValueType> operandStack,
            List<ValueType> dummyOperands,
            List<ValueType> extraSaveTypes
    ) throws WasmAssemblerException {
        StringBuilder b = new StringBuilder();
        for (JavaType discard : discardTypes) {
            b.append(discard.toJvmDescriptor());
        }
        b.append(":");

        for (ValueType operand : operandStack) {
            b.append(WasmTypeConverter.toJavaType(operand).toJvmDescriptor());
        }

        b.append(":");
        for (ValueType operand : dummyOperands) {
            b.append(WasmTypeConverter.toJavaType(operand).toJvmDescriptor());
        }

        b.append(":");
        for (ValueType operand : extraSaveTypes) {
            b.append(WasmTypeConverter.toJavaType(operand).toJvmDescriptor());
        }

        return b.toString();
    }

    public static final class PointAndLabel {
        private final ContinuationCutPoint point;
        private final CodeLabel pauseLabel;

        private PointAndLabel(ContinuationCutPoint point, CodeLabel pauseLabel) {
            this.point = point;
            this.pauseLabel = pauseLabel;
        }

        public ContinuationCutPoint getPoint() {
            return point;
        }

        public CodeLabel getPauseLabel() {
            return pauseLabel;
        }
    }
}
