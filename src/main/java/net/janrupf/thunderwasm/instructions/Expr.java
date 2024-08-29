package net.janrupf.thunderwasm.instructions;

import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.util.CommonAlgorithms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A collection of instructions that form an expression.
 */
public final class Expr {
    private final List<InstructionInstance> instructions;

    public Expr(List<InstructionInstance> instructions) {
        this.instructions = instructions;
    }

    /**
     * Retrieves the instructions of this expression.
     *
     * @return the instructions of this expression
     */
    public List<InstructionInstance> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }

    @Override
    public String toString() {
        return "<" + instructions.size() + " instructions>";
    }

    /**
     * Read an expression from the given loader.
     *
     * @param loader the loader to read the data from
     * @return the read expression
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public static Expr read(WasmLoader loader) throws IOException, InvalidModuleException {
        return readPair(loader, false).getPrimary();
    }

    /**
     * Read a pair of expressions from the given loader.
     * <p>
     * The secondary expression may be null if the primary expression ends with an "end" and not
     * an else.
     *
     * @param loader the loader to read the data from
     * @return the read pair of expressions
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public static Pair readPair(WasmLoader loader) throws IOException, InvalidModuleException {
        return readPair(loader, true);
    }

    /**
     * Read a pair of expressions from the given loader.
     * <p>
     * The secondary expression may be null if the primary expression ends with an "end" and not
     * an else.
     * <p>
     * If no secondary instruction is allowed, the secondary expression will always be null.
     *
     * @param loader         the loader to read the data from
     * @param allowSecondary whether a secondary expression is allowed
     * @return the read pair of expressions
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public static Pair readPair(WasmLoader loader, boolean allowSecondary) throws IOException, InvalidModuleException {
        List<InstructionInstance> primaryInstructions = new ArrayList<>();
        List<InstructionInstance> secondaryInstructions;

        byte opCode = readInstructionsUntil(loader, primaryInstructions, (byte) 0x0B, (byte) 0x05);

        if (opCode == 0x05) {
            if (!allowSecondary) {
                throw new InvalidModuleException(
                        "Expression ended with else but secondary expression is not allowed"
                );
            }

            secondaryInstructions = new ArrayList<>();
            readInstructionsUntil(loader, secondaryInstructions, (byte) 0x0B);
        } else {
            secondaryInstructions = null;
        }

        return new Pair(
                new Expr(primaryInstructions),
                secondaryInstructions == null ? null : new Expr(secondaryInstructions)
        );
    }

    /**
     * Read instructions until one of the given endings is reached.
     *
     * @param loader       the loader to read the data from
     * @param instructions the list to add the instructions to
     * @param endings      the endings to stop at
     * @return the ending that was reached
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    private static byte readInstructionsUntil(
            WasmLoader loader,
            List<InstructionInstance> instructions,
            byte... endings
    ) throws IOException, InvalidModuleException {
        while (true) {
            byte opCode = loader.requireByte();
            if (CommonAlgorithms.byteArrayContains(endings, opCode)) {
                return opCode;
            }

            instructions.add(loader.readInstructionData(opCode));
        }
    }

    /**
     * A pair of expressions.
     */
    public static final class Pair {
        private final Expr primary;
        private final Expr secondary;

        private Pair(Expr primary, Expr secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        /**
         * Retrieves the primary expression.
         *
         * @return the primary expression
         */
        public Expr getPrimary() {
            return primary;
        }

        /**
         * Retrieves the secondary expression.
         *
         * @return the secondary expression
         */
        public Expr getSecondary() {
            return secondary;
        }
    }
}
