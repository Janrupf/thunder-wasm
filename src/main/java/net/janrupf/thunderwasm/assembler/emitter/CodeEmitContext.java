package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.WasmPushedLabel;
import net.janrupf.thunderwasm.lookup.ElementLookups;
import net.janrupf.thunderwasm.assembler.WasmFrameState;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the context in which code is emitted.
 */
public final class CodeEmitContext {
    private final ElementLookups lookups;
    private final CodeEmitter emitter;
    private final List<WasmFrameState> frameStates;
    private final List<WasmPushedLabel> blockJumpLabels;
    private final WasmGenerators generators;
    private final LocalVariables localVariables;

    public CodeEmitContext(
            ElementLookups lookups,
            CodeEmitter emitter,
            WasmFrameState frameState,
            WasmGenerators generators,
            LocalVariables localVariables
    ) {
        this(
                lookups,
                emitter,
                frameState,
                null,
                generators,
                localVariables
        );
    }

    public CodeEmitContext(
            ElementLookups lookups,
            CodeEmitter emitter,
            WasmFrameState frameState,
            WasmPushedLabel endLabel,
            WasmGenerators generators,
            LocalVariables localVariables
    ) {
        this.lookups = lookups;
        this.emitter = emitter;
        this.frameStates = new ArrayList<>();
        this.frameStates.add(frameState);
        this.blockJumpLabels = new ArrayList<>();
        this.blockJumpLabels.add(endLabel);
        this.generators = generators;
        this.localVariables = localVariables;
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
     * Retrieves the emitter that is used to emit code.
     *
     * @return the emitter
     */
    public CodeEmitter getEmitter() {
        return emitter;
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
     * Retrieves the block jump label of the context.
     *
     * @return the block jump label, or null, if inside the top level block
     */
    public WasmPushedLabel getBlockJumpLabel() {
        return getBlockJumpLabel(0);
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
}
