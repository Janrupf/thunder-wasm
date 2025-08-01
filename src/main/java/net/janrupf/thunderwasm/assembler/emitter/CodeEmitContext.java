package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.WasmAssemblerConfiguration;
import net.janrupf.thunderwasm.assembler.WasmPushedLabel;
import net.janrupf.thunderwasm.assembler.analysis.AnalysisResult;
import net.janrupf.thunderwasm.assembler.continuation.ContinuationContext;
import net.janrupf.thunderwasm.lookup.ElementLookups;
import net.janrupf.thunderwasm.assembler.WasmFrameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the context in which code is emitted.
 */
public final class CodeEmitContext {
    private final String blockNamePrefix;
    private final AnalysisResult analysisResult;
    private final ClassFileEmitter classFileEmitter;
    private final CodeEmitter emitter;
    private final ElementLookups lookups;
    private final List<WasmFrameState> frameStates;
    private final List<WasmPushedLabel> blockJumpLabels;
    private final WasmGenerators generators;
    private final LocalVariables localVariables;
    private final WasmAssemblerConfiguration configuration;
    private final LocalGadgets localGadgets;
    private final ContinuationContext continuationContext;

    private int blockNameCounter;

    public CodeEmitContext(
            String blockNamePrefix,
            AnalysisResult analysisResult,
            ClassFileEmitter classFileEmitter,
            CodeEmitter emitter,
            ElementLookups lookups,
            WasmFrameState frameState,
            WasmGenerators generators,
            LocalVariables localVariables,
            WasmAssemblerConfiguration configuration
    ) {
        this(
                blockNamePrefix,
                analysisResult,
                classFileEmitter,
                emitter,
                lookups,
                frameState,
                Collections.emptyList(),
                generators,
                localVariables,
                configuration
        );
    }

    public CodeEmitContext(
            String blockNamePrefix,
            AnalysisResult analysisResult,
            ClassFileEmitter classFileEmitter,
            CodeEmitter emitter,
            ElementLookups lookups,
            WasmFrameState frameState,
            List<WasmPushedLabel> alreadyPushedLabels,
            WasmGenerators generators,
            LocalVariables localVariables,
            WasmAssemblerConfiguration configuration
    ) {
        this.blockNamePrefix = blockNamePrefix;
        this.analysisResult = analysisResult;
        this.classFileEmitter = classFileEmitter;
        this.lookups = lookups;
        this.emitter = emitter;
        this.frameStates = new ArrayList<>();
        if (frameState != null) {
            this.frameStates.add(frameState);
        }
        this.blockJumpLabels = new ArrayList<>();
        this.blockJumpLabels.addAll(alreadyPushedLabels);
        this.generators = generators;
        this.localVariables = localVariables;
        this.configuration = configuration;
        this.localGadgets = new LocalGadgets();
        this.continuationContext = new ContinuationContext();
    }

    /**
     * Retrieves the next block name.
     *
     * @return the next block name
     */
    public String nextBlockName() {
        if (blockNamePrefix == null) {
            throw new IllegalStateException("Split block generation not allowed in this context");
        }

        return blockNamePrefix + blockNameCounter++;
    }

    /**
     * Retrieves the result of the code analysis.
     *
     * @return the code analysis result
     */
    public AnalysisResult getAnalysisResult() {
        if (analysisResult == null) {
            throw new IllegalStateException("No analysis has been run for this context");
        }

        return analysisResult;
    }

    /**
     * Retrieves the class file emitter that is owning the code container.
     *
     * @return the class file emitter
     */
    public ClassFileEmitter getClassFileEmitter() {
        return classFileEmitter;
    }

    /**
     * Retrieves the emitter that is used to emit code.
     *
     * @return the emitter
     */
    public CodeEmitter getEmitter() {
        return emitter;
    }

    /**
     * Retrieves the lookups that are used to look up elements.
     *
     * @return the lookups
     */
    public ElementLookups getLookups() {
        return lookups;
    }

    /**
     * Retrieves the frame state of the context.
     *
     * @return the frame state
     */
    public WasmFrameState getFrameState() {
        return getFrameState(0);
    }

    /**
     * Retrieves the frame state of the context at a given depth.
     *
     * @param depth the frame state at the given depth
     * @return the frame state
     */
    public WasmFrameState getFrameState(int depth) {
        return frameStates.get(frameStates.size() - 1 - depth);
    }

    /**
     * Retrieve all current frame states.
     *
     * @return all frame states
     */
    public List<WasmFrameState> getAllFrameStates() {
        return Collections.unmodifiableList(frameStates);
    }

    /**
     * Retrieves the block jump label of the context.
     *
     * @return the block jump label, or null, if inside the top level block
     */
    public WasmPushedLabel getBlockJumpLabel() {
        return getBlockJumpLabel(0);
    }

    /**
     * Retrieves all currently available block jump labels.
     *
     * @return all block jump labels
     */
    public List<WasmPushedLabel> getAllBlockJumpLabels() {
        return blockJumpLabels;
    }

    /**
     * Retrieves the block jump label of the context.
     *
     * @param depth how many labels to go up
     * @return the block jump label, or null, if no label exists at the given depth
     */
    public WasmPushedLabel getBlockJumpLabel(int depth) {
        if (blockJumpLabels.size() <= depth) {
            return null;
        }

        return blockJumpLabels.get(blockJumpLabels.size() - 1 - depth);
    }

    /**
     * Retrieves the generators that are used to generate code.
     *
     * @return the generators
     */
    public WasmGenerators getGenerators() {
        return generators;
    }

    /**
     * Retrieves the local variables of the context.
     *
     * @return the local variables
     */
    public LocalVariables getLocalVariables() {
        return localVariables;
    }

    /**
     * Retrieves the assembler configuration.
     *
     * @return the assembler configuration
     */
    public WasmAssemblerConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Push a new block.
     *
     * @param frameState    the new frame state
     * @param blockEndLabel the label to which branches to the block jump
     */
    public void pushBlock(WasmFrameState frameState, WasmPushedLabel blockEndLabel) {
        frameStates.add(frameState);
        blockJumpLabels.add(blockEndLabel);
    }

    /**
     * Pop a block.
     */
    public void popBlock() {
        frameStates.remove(frameStates.size() - 1);
        blockJumpLabels.remove(blockJumpLabels.size() - 1);
    }

    /**
     * Restore a previous frame state after the previous one was invalidated by a branch.
     *
     * @param state the state to restore to
     */
    public void restoreFrameStateAfterBranch(WasmFrameState state) {
        frameStates.set(frameStates.size() - 1, state);
    }

    /**
     * Retrieves the gadgets that are available.
     *
     * @return the local gadgets
     */
    public LocalGadgets getLocalGadgets() {
        return localGadgets;
    }

    /**
     * Retrieves the continuation context.
     *
     * @return the continuation context
     */
    public ContinuationContext getContinuationContext() {
        return continuationContext;
    }
}
