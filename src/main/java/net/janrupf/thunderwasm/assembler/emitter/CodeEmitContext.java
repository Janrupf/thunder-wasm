package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
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
    private final List<CodeLabel> blockEndLabels;
    private final WasmGenerators generators;

    public CodeEmitContext(
            ElementLookups lookups,
            CodeEmitter emitter,
            WasmFrameState frameState,
            WasmGenerators generators
    ) {
        this.lookups = lookups;
        this.emitter = emitter;
        this.frameStates = new ArrayList<>();
        this.frameStates.add(frameState);
        this.blockEndLabels = new ArrayList<>();
        this.generators = generators;
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
        return frameStates.get(frameStates.size() - 1);
    }

    /**
     * Retrieves the block end label of the context.
     *
     * @return the block end label, or null, if inside the top level block
     */
    public CodeLabel getBlockEndLabel() {
        if (blockEndLabels.isEmpty()) {
            return null;
        }

        return blockEndLabels.get(blockEndLabels.size() - 1);
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
     * Push a new block.
     *
     * @param frameState the new frame state
     * @param blockEndLabel the label at which the block ends
     */
    public void pushBlock(WasmFrameState frameState, CodeLabel blockEndLabel) {
        frameStates.add(frameState);
        blockEndLabels.add(blockEndLabel);
    }

    /**
     * Pop a block.
     *
     * @throws WasmAssemblerException if the block can not be popped
     */
    public void popBlock() throws WasmAssemblerException {
        if (frameStates.size() < 2) {
            throw new WasmAssemblerException("Can't pop the top level block");
        }

        frameStates.remove(frameStates.size() - 1);
        blockEndLabels.remove(frameStates.size() - 1);
    }
}
