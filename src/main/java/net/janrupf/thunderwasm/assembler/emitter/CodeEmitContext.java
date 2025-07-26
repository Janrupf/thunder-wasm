package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.JavaFrameSnapshot;
import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmPushedLabel;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.lookup.ElementLookups;
import net.janrupf.thunderwasm.assembler.WasmFrameState;

import java.util.ArrayList;
import java.util.Comparator;
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
     * Convert a frame snapshot that has been inferred from WASM state to a real Java frame snapshot.
     * <p>
     * Frame snapshots inferred from the WASM state don't take into account the order of arguments
     * and the 'this' local. This function re-orders the locals to match the expected order.
     *
     * @param snapshot the frame snapshot to fix up
     * @return the fixed up Java frame snapshot
     * @throws WasmAssemblerException if the frame snapshot can not be fixed up
     */
    public JavaFrameSnapshot fixupInferredFrameSnapshot(JavaFrameSnapshot snapshot) throws WasmAssemblerException {
        // We need to re-order the static locals and `this` - the first N locals of the snapshot always need to
        // be the static locals and this. Additional locals may have been allocated later and are not re-ordered
        // by this emitter.
        List<JavaType> newLocals = new ArrayList<>(snapshot.getLocals());

        JavaLocal thisLocal = localVariables.getThis();
        List<JavaLocal> staticLocals = localVariables.getAllStatic();

        List<JavaLocal> specialLocals = new ArrayList<>(staticLocals.size() + (thisLocal != null ? 1 : 0));
        if (thisLocal != null) {
            specialLocals.add(thisLocal);
        }
        specialLocals.addAll(staticLocals);

        if (newLocals.size() < specialLocals.size() - 1) {
            // This shouldn't happen unless someone freed the argument locals, static locals or the 'this' local.
            throw new WasmAssemblerException("Some locals have unexpectedly been freed, this should not happen");
        }

        // Sort by slot index and overwrite the re-ordered locals
        specialLocals.sort(Comparator.comparingInt(JavaLocal::getSlot));

        // Make room for the additional this local
        if (thisLocal != null) {
            newLocals.add(0, ObjectType.OBJECT);
        }
        for (int i = 0; i < specialLocals.size(); i++) {
            newLocals.set(i, specialLocals.get(i).getType());
        }

        // The emitter doesn't do anything special with the stack, just use it as is
        return new JavaFrameSnapshot(snapshot.getStack(), newLocals);
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
