package net.janrupf.thunderwasm.test.wast.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * WASM test command for asserting that a module is malformed.
 * Represents assert_malformed test assertions in WAST test definitions.
 * These tests verify that modules with syntax errors or structural problems
 * are properly rejected during parsing.
 */
public final class AssertMalformedCommand extends WastCommand {
    private final String filename;
    private final String text;
    private final String moduleType;
    
    @JsonCreator
    public AssertMalformedCommand(
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
     * Gets the filename of the malformed module to test.
     *
     * @return the module filename (e.g., "address.1.wat", "invalid.wasm")
     */
    public String getFilename() {
        return filename;
    }
    
    /**
     * Gets the expected error message or description.
     *
     * @return the expected error text describing why the module is malformed
     */
    public String getText() {
        return text;
    }
    
    /**
     * Gets the module type being tested.
     *
     * @return the module type ("text" for .wat files, "binary" for .wasm files)
     */
    public String getModuleType() {
        return moduleType;
    }

    @Override
    public String getTestDisplayMeta() {
        return text;
    }

    @Override
    public String toString() {
        return "assert_malformed " + filename + " (" + moduleType + ") - " + text + " at line " + getLine();
    }
}