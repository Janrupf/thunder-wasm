package net.janrupf.thunderwasm.test.wast.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * WASM test command for asserting that a module cannot be instantiated.
 * Represents assert_uninstantiable test assertions in WAST test definitions.
 * These tests verify that modules fail at instantiation time due to runtime
 * issues like out of bounds memory access, resource limits, etc.
 */
public final class AssertUninstantiableCommand extends WastCommand {
    private final String filename;
    private final String text;
    private final String moduleType;
    
    @JsonCreator
    public AssertUninstantiableCommand(
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
     * Gets the filename of the module that should fail to instantiate.
     *
     * @return the module filename (e.g., "data.27.wasm")
     */
    public String getFilename() {
        return filename;
    }
    
    /**
     * Gets the expected error message or description.
     *
     * @return the expected error text describing why instantiation should fail
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
    
    /**
     * Checks if this is testing a text format module (.wat file).
     *
     * @return true if testing a text format module, false otherwise
     */
    public boolean isTextModule() {
        return "text".equals(moduleType);
    }
    
    /**
     * Checks if this is testing a binary format module (.wasm file).
     *
     * @return true if testing a binary format module, false otherwise
     */
    public boolean isBinaryModule() {
        return "binary".equals(moduleType);
    }
    
    @Override
    public String toString() {
        return "assert_uninstantiable " + filename + " (" + moduleType + ") - " + text + " at line " + getLine();
    }
}