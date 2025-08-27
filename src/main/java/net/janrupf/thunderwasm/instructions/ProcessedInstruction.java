package net.janrupf.thunderwasm.instructions;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;

/**
 * Interface for processed instructions that split validation and code generation.
 * <p>
 * Allows proper handling of unreachable instructions by separating input processing,
 * bytecode emission, and output processing into distinct phases.
 */
public interface ProcessedInstruction {
    /**
     * Emit Java bytecode for this instruction.
     * <p>
     * Only called when the frame state is reachable.
     *
     * @param context the code emit context
     * @throws WasmAssemblerException if bytecode emission fails
     */
    void emitBytecode(CodeEmitContext context) throws WasmAssemblerException;

    /**
     * Process the instruction in an unreachable place.
     * <p>
     * Only called when the frame state is unreachable.
     * <p>
     * Most instructions probably don't care about this and can leave it as a no-op.
     * However, instructions that affect control flow (like blocks, loops, and branches)
     * may need to handle this case to maintain correct validation and state.
     *
     * @param context the code emit context
     * @throws WasmAssemblerException if processing fails
     */
    default void processUnreachable(CodeEmitContext context) throws WasmAssemblerException {}

    /**
     * Process outputs and update the frame state.
     * <p>
     * Always called, even for unreachable instructions, to maintain proper validation.
     *
     * @param context the code emit context
     * @throws WasmAssemblerException if output processing fails
     */
    void processOutputs(CodeEmitContext context) throws WasmAssemblerException;
}