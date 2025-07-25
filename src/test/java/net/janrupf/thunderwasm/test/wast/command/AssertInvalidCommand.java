package net.janrupf.thunderwasm.test.wast.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * WASM test command for asserting that a module is invalid.
 * Represents assert_invalid test assertions in WAST test definitions.
 * These tests verify that modules with semantic errors or validation failures
 * are properly rejected during validation (after successful parsing).
 */
public final class AssertInvalidCommand extends WastCommand {
    private final String filename;
    private final String text;
    private final String moduleType;
    
    @JsonCreator
    public AssertInvalidCommand(
            @JsonProperty("line") int line,
            @JsonProperty("filename") String filename,
            @JsonProperty("text") String text,
            @JsonProperty("module_type") String moduleType) {
        super(line);
        this.filename = filename;
        this.text = text;
        this.moduleType = moduleType;
    }
    
    /**
     * Gets the filename of the invalid module to test.
     *
     * @return the module filename (e.g., "align.69.wasm", "invalid.wat")
     */
    public String getFilename() {
        return filename;
    }
    
    /**
     * Gets the expected error message or description.
     *
     * @return the expected error text describing why the module is invalid
     */
    public String getText() {
        return text;
    }

    @Override
    public String getTestDisplayMeta() {
        return text;
    }

    @Override
    public String toString() {
        return "assert_invalid " + filename + " (" + moduleType + ") - " + text + " at line " + getLine();
    }
}