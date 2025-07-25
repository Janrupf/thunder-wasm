package net.janrupf.thunderwasm.test.wast.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.janrupf.thunderwasm.test.wast.action.WastAction;
import net.janrupf.thunderwasm.test.wast.value.WastValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * WASM test command for asserting that an action traps with a specific error message.
 * Represents assert_trap test assertions in WAST test definitions.
 */
public final class AssertTrapCommand extends WastCommand {
    private final WastAction action;
    private final String text;
    private final List<WastValue> expected;
    
    @JsonCreator
    public AssertTrapCommand(
            @JsonProperty("line") int line,
            @JsonProperty("action") WastAction action,
            @JsonProperty("text") String text,
            @JsonProperty("expected") List<WastValue> expected) {
        super(line);
        this.action = action;
        this.text = text;
        this.expected = expected != null ? new ArrayList<>(expected) : Collections.emptyList();
    }
    
    /**
     * Gets the action to execute for this assertion.
     *
     * @return the WastAction that should trap
     */
    public WastAction getAction() {
        return action;
    }
    
    /**
     * Gets the expected trap/error message.
     *
     * @return the expected error message text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Gets the list of expected types (usually empty for traps).
     * This field exists for completeness but typically contains only type information
     * since the action traps before returning actual values.
     *
     * @return immutable list of expected WastValue types
     */
    public List<WastValue> getExpected() {
        return expected;
    }

    @Override
    public String getTestDisplayMeta() {
        return text + " -> " + action.toString();
    }

    @Override
    public String toString() {
        return "assert_trap " + action + " with message \"" + text + "\" at line " + getLine();
    }
}