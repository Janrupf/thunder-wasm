package net.janrupf.thunderwasm.test.wast.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.janrupf.thunderwasm.test.wast.action.WastAction;
import net.janrupf.thunderwasm.test.wast.value.WastValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * WASM test command for asserting that an action causes stack exhaustion.
 * Represents assert_exhaustion test assertions in WAST test definitions.
 * These tests verify that actions (like recursive function calls) properly
 * trigger stack overflow errors.
 */
public final class AssertExhaustionCommand extends WastCommand {
    private final WastAction action;
    private final String text;
    private final List<WastValue> expected;
    
    @JsonCreator
    public AssertExhaustionCommand(
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
     * @return the WastAction that should cause stack exhaustion
     */
    public WastAction getAction() {
        return action;
    }
    
    /**
     * Gets the expected exhaustion/error message.
     *
     * @return the expected error message text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Gets the list of expected types (usually empty for exhaustion).
     * This field exists for completeness but typically contains only type information
     * since the action exhausts the stack before returning actual values.
     *
     * @return immutable list of expected WastValue types
     */
    public List<WastValue> getExpected() {
        return expected;
    }

    @Override
    public String getTestDisplayMeta() {
        return text;
    }

    @Override
    public String toString() {
        return "assert_exhaustion " + action + " with message \"" + text + "\" at line " + getLine();
    }
}