package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.instructions.Expr;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.BlockType;

import java.io.IOException;

public final class BlockData implements WasmInstruction.Data {
    private final BlockType type;
    private final Expr primaryExpression;
    private final Expr secondaryExpression;

    private BlockData(BlockType type, Expr primaryExpression, Expr secondaryExpression) {
        this.type = type;
        this.primaryExpression = primaryExpression;
        this.secondaryExpression = secondaryExpression;
    }

    /**
     * Retrieves the type of the block.
     *
     * @return the type of the block
     */
    public BlockType getType() {
        return type;
    }

    /**
     * Retrieves the primary expression.
     * <p>
     * The primary expression is always present.
     *
     * @return the primary instruction block
     */
    public Expr getPrimaryExpression() {
        return primaryExpression;
    }

    /**
     * Retrieves the secondary expression.
     * <p>
     * The secondary expression is only valid for instructions which branch on blocks.
     *
     * @return the secondary instruction block
     */
    public Expr getSecondaryExpression() {
        return secondaryExpression;
    }

    @Override
    public String toString() {
        if (secondaryExpression == null) {
            return "(" + primaryExpression + ")";
        } else {
            return "(" + primaryExpression + ", " + secondaryExpression + ")";
        }
    }

    /**
     * Reads a block data from the given loader.
     *
     * @param loader the loader to read the data from
     * @return the block data
     * @throws IOException            if an I/O error occurs
     * @throws InvalidModuleException if the module is invalid
     */
    public static BlockData read(WasmLoader loader, boolean allowSecondary) throws IOException, InvalidModuleException {
        BlockType type = loader.readBlockType();
        Expr.Pair expressionPair = Expr.readPair(loader, allowSecondary);

        return new BlockData(type, expressionPair.getPrimary(), expressionPair.getSecondary());
    }
}
