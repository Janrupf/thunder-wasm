package net.janrupf.thunderwasm.test.wast.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base class for WASM test commands that can be executed in a test suite.
 * Commands represent various operations like module loading, assertions, and actions.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ModuleCommand.class, name = "module"),
    @JsonSubTypes.Type(value = RegisterCommand.class, name = "register"),
    @JsonSubTypes.Type(value = AssertReturnCommand.class, name = "assert_return"),
    @JsonSubTypes.Type(value = AssertTrapCommand.class, name = "assert_trap"),
    @JsonSubTypes.Type(value = AssertMalformedCommand.class, name = "assert_malformed"),
    @JsonSubTypes.Type(value = AssertInvalidCommand.class, name = "assert_invalid"),
    @JsonSubTypes.Type(value = AssertExhaustionCommand.class, name = "assert_exhaustion"),
    @JsonSubTypes.Type(value = AssertUninstantiableCommand.class, name = "assert_uninstantiable"),
    @JsonSubTypes.Type(value = AssertUnlinkableCommand.class, name = "assert_unlinkable"),
    @JsonSubTypes.Type(value = ActionCommand.class, name = "action")
})
public abstract class WastCommand {
    private final int line;
    
    protected WastCommand(@JsonProperty("line") int line) {
        this.line = line;
    }
    
    /**
     * Gets the line number in the original WAST file where this command appears.
     * Useful for test reporting and debugging.
     *
     * @return the line number
     */
    public int getLine() {
        return line;
    }
}
