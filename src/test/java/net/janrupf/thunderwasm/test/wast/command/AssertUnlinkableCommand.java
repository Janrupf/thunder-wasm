package net.janrupf.thunderwasm.test.wast.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * WASM test command for asserting that a module cannot be linked.
 * Represents assert_unlinkable test assertions in WAST test definitions.
 * These tests verify that modules fail at linking time due to missing
 * imports, type mismatches, or other link-time compatibility issues.
 */
public final class AssertUnlinkableCommand extends WastCommand {
    private final String filename;
    private final String text;
    private final String moduleType;
    
    @JsonCreator
    public AssertUnlinkableCommand(
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
     * Gets the filename of the module that should fail to link.
     *
     * @return the module filename (e.g., "data.27.wasm")
     */
    public String getFilename() {
        return filename;
    }
    
    /**
     * Gets the expected error message or description.
     *
     * @return the expected error text describing why linking should fail
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
        return "assert_unlinkable " + filename + " (" + moduleType + ") - " + text + " at line " + getLine();
    }
}