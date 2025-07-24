package net.janrupf.thunderwasm.test.wast.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.janrupf.thunderwasm.test.wast.action.WastAction;
import net.janrupf.thunderwasm.test.wast.value.WastValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * WASM test command for executing an action without specific assertions.
 * Represents simple action execution in WAST test definitions.
 * These commands execute actions (like function calls) that may have side effects
 * but don't require specific return value verification.
 */
public final class ActionCommand extends WastCommand {
    private final WastAction action;
    private final List<WastValue> expected;
    
    @JsonCreator
    public ActionCommand(
            @JsonProperty("line") int line,
            @JsonProperty("action") WastAction action,
            @JsonProperty("expected") List<WastValue> expected) {
        super(line);
        this.action = action;
        this.expected = expected != null ? new ArrayList<>(expected) : Collections.emptyList();
    }
    
    /**
     * Gets the action to execute.
     *
     * @return the WastAction to execute
     */
    public WastAction getAction() {
        return action;
    }
    
    /**
     * Gets the list of expected return values.
     * Usually empty for actions that don't return values or don't need assertion.
     *
     * @return immutable list of expected WastValue results
     */
    public List<WastValue> getExpected() {
        return expected;
    }
    
    /**
     * Checks if this action expects no return values (void return).
     *
     * @return true if no return values are expected, false otherwise
     */
    public boolean expectsVoid() {
        return expected.isEmpty();
    }
    
    /**
     * Checks if this action has expected return values.
     *
     * @return true if return values are expected, false otherwise
     */
    public boolean hasExpected() {
        return !expected.isEmpty();
    }
    
    @Override
    public String toString() {
        if (expectsVoid()) {
            return "action " + action + " (void) at line " + getLine();
        } else {
            return "action " + action + " expects " + expected.size() + " values at line " + getLine();
        }
    }
}