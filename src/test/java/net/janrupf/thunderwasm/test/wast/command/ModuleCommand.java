package net.janrupf.thunderwasm.test.wast.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * WASM test command for loading a module from a file.
 * Represents module loading operations in WAST test definitions.
 */
public final class ModuleCommand extends WastCommand {
    private final String filename;
    private final String name;
    
    @JsonCreator
    public ModuleCommand(
            @JsonProperty("line") int line,
            @JsonProperty("filename") String filename,
            @JsonProperty("name") String name) {
        super(line);
        this.filename = filename;
        this.name = name;
    }
    
    /**
     * Gets the filename of the WASM module to load.
     *
     * @return the module filename (e.g., "address.0.wasm")
     */
    public String getFilename() {
        return filename;
    }
    
    /**
     * Gets the optional name assigned to this module.
     * Some modules are named for later reference in tests.
     *
     * @return the module name, or null if unnamed
     */
    public String getName() {
        return name;
    }
    
    /**
     * Checks if this module has a name.
     *
     * @return true if the module has a name, false otherwise
     */
    public boolean isNamed() {
        return name != null && !name.isEmpty();
    }

    @Override
    public String getTestDisplayMeta() {
        if (name == null) {
            return getFilename();
        } else {
            return getFilename() + " (as " + name + ")";
        }
    }

    @Override
    public String toString() {
        if (isNamed()) {
            return "module " + name + " (" + filename + ") at line " + getLine();
        } else {
            return "module (" + filename + ") at line " + getLine();
        }
    }
}