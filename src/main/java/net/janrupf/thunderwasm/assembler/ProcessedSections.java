package net.janrupf.thunderwasm.assembler;

/**
 * Used by the assembler to keep track of which sections it has already processed.
 */
public enum ProcessedSections {
    GLOBAL,
    CODE,
    MEMORY,
}
