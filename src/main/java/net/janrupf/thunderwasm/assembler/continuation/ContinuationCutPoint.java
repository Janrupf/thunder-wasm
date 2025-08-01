package net.janrupf.thunderwasm.assembler.continuation;

import net.janrupf.thunderwasm.assembler.emitter.CodeLabel;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.types.ValueType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ContinuationCutPoint {
    private final List<JavaType> discardTypes;
    private final List<ValueType> operandStack;
    private final List<ValueType> dummyOperands;
    private final List<ValueType> extraSaveTypes;
    private final List<JavaLocal> extraSaveLocals;
    private final Map<Integer, CodeLabel> pauseLabels;
    private final Map<Integer, CodeLabel> resumeLabels;

    public ContinuationCutPoint(
            List<JavaType> discardTypes,
            List<ValueType> operandStack,
            List<ValueType> dummyOperands,
            List<ValueType> extraSaveTypes,
            List<JavaLocal> extraSaveLocals
    ) {
        this.discardTypes = discardTypes;
        this.operandStack = operandStack;
        this.dummyOperands = dummyOperands;
        this.extraSaveTypes = extraSaveTypes;
        this.extraSaveLocals = extraSaveLocals;
        this.pauseLabels = new HashMap<>();
        this.resumeLabels = new HashMap<>();
    }

    public List<JavaType> getDiscardTypes() {
        return discardTypes;
    }

    public List<ValueType> getOperandStack() {
        return operandStack;
    }

    public List<ValueType> getDummyOperands() {
        return dummyOperands;
    }

    public List<ValueType> getExtraSaveTypes() {
        return extraSaveTypes;
    }

    public List<JavaLocal> getExtraSaveLocals() {
        return extraSaveLocals;
    }

    public void addResumePoint(int id, CodeLabel resumeLabel, CodeLabel pauseLabel) {
        this.resumeLabels.put(id, resumeLabel);
        this.pauseLabels.put(id, pauseLabel);
    }

    public Map<Integer, CodeLabel> getPauseLabels() {
        return pauseLabels;
    }

    public Map<Integer, CodeLabel> getResumeLabels() {
        return resumeLabels;
    }
}
