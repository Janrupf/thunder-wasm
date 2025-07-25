package net.janrupf.thunderwasm.test.wast.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.janrupf.thunderwasm.test.wast.action.WastAction;
import net.janrupf.thunderwasm.test.wast.value.WastValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * WASM test command for asserting that an action returns expected values.
 * Represents assert_return test assertions in WAST test definitions.
 */
public final class AssertReturnCommand extends WastCommand {
    private final WastAction action;
    private final List<WastValue> expected;
    
    @JsonCreator
    public AssertReturnCommand(
            @JsonProperty("line") int line,
            @JsonProperty("action") WastAction action,
            @JsonProperty("expected") List<WastValue> expected) {
        super(line);
        this.action = action;
        this.expected = expected != null ? new ArrayList<>(expected) : Collections.emptyList();
    }
    
    /**
     * Gets the action to execute for this assertion.
     *
     * @return the WastAction to execute
     */
    public WastAction getAction() {
        return action;
    }
    
    /**
     * Gets the list of expected return values.
     *
     * @return immutable list of expected WastValue results
     */
    public List<WastValue> getExpected() {
        return expected;
    }

    @Override
    public String getTestDisplayMeta() {
        return action.toString();
    }

    @Override
    public String toString() {
        return "assert_return " + action + " expects " + expected.size() + " values at line " + getLine();
    }
}