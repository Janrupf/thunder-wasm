package net.janrupf.thunderwasm.instructions;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.analysis.AnalysisContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.eval.EvalContext;
import net.janrupf.thunderwasm.instructions.decoder.InstructionDecoder;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;
import java.util.Objects;

public abstract class WasmInstruction<D extends WasmInstruction.Data> {
    private final String name;
    private final byte opCode;

    public WasmInstruction(String name, byte opCode) {
        this.name = name;
        this.opCode = opCode;
    }

    /**
     * Retrieves the name of the instruction.
     *
     * @return the name of the instruction
     */
    public final String getName() {
        return name;
    }

    /**
     * Retrieves the opcode of the instruction.
     *
     * @return the opcode of the instruction
     */
    public final byte getOpCode() {
        return opCode;
    }

    /**
     * Retrieves the decoder for this instruction.
     * <p>
     * By default, this is an {@link InstructionDecoder} that only checks the opcode.
     *
     * @return the decoder
     */
    public InstructionDecoder getDecoder() {
        return InstructionDecoder.opCodeOnly(this);
    }

    @Override
    public final String toString() {
        return getName() + " (0x" + Integer.toUnsignedString(((int) getOpCode()) & 0xFF, 16) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WasmInstruction)) return false;
        WasmInstruction<?> that = (WasmInstruction<?>) o;
        return opCode == that.opCode && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, opCode);
    }

    /**
     * Read the instruction data after the opcode.
     *
     * @param loader the loader to read the data from
     * @return the data
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public abstract D readData(WasmLoader loader) throws IOException, InvalidModuleException;

    /**
     * Emit code for the instruction.
     *
     * @param context the code emit context
     * @param data    the instruction data
     * @throws WasmAssemblerException if emitting the code fails
     */
    public void emitCode(
            CodeEmitContext context,
            D data
    ) throws WasmAssemblerException {
        throw new WasmAssemblerException(
                "Code emitter not implemented for " + getName(),
                new UnsupportedOperationException("TODO")
        );
    }

    /**
     * Contribute data to the code analysis.
     *
     * @param context the context in which the analysis happens
     * @param data    the instruction data
     * @throws WasmAssemblerException if analysis fails
     */
    public void runAnalysis(
            AnalysisContext context,
            D data
    ) throws WasmAssemblerException {
        // No-op by default
    }

    /**
     * Check if this instruction is a constant instruction.
     *
     * @return true if this instruction is a constant instruction
     */
    public boolean isConst() {
        return false;
    }

    /**
     * Evaluate the instruction.
     *
     * @param context the evaluation context
     * @param data    the instruction data
     * @throws WasmAssemblerException if the evaluation fails
     */
    public void eval(EvalContext context, D data) throws WasmAssemblerException {
        throw new WasmAssemblerException(
                "Evaluator not implemented for " + getName(),
                new UnsupportedOperationException("TODO")
        );
    }

    /**
     * Marker interface for instruction data.
     */
    public interface Data {
    }
}
