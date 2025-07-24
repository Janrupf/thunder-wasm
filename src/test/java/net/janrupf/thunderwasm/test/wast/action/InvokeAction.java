package net.janrupf.thunderwasm.test.wast.action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.janrupf.thunderwasm.test.wast.value.WastValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * WASM test action for invoking a function with arguments.
 * Represents function calls in WAST test definitions.
 */
public final class InvokeAction extends WastAction {
    private final String field;
    private final List<WastValue> args;
    
    @JsonCreator
    public InvokeAction(
            @JsonProperty("module") String module,
            @JsonProperty("field") String field,
            @JsonProperty("args") List<WastValue> args) {
        super("invoke", module);
        this.field = field;
        this.args = args != null ? new ArrayList<>(args) : Collections.emptyList();
    }

    @Override
    public String getField() {
        return field;
    }
    
    /**
     * Gets the list of arguments to pass to the function.
     *
     * @return immutable list of WastValue arguments
     */
    public List<WastValue> getArgs() {
        return args;
    }
    
    /**
     * Gets the number of arguments.
     *
     * @return the argument count
     */
    public int getArgCount() {
        return args.size();
    }
    
    /**
     * Gets a specific argument by index.
     *
     * @param index the argument index
     * @return the WastValue at the specified index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public WastValue getArg(int index) {
        return args.get(index);
    }
    
    @Override
    public String toString() {
        return "invoke " + field + "(" + args.size() + " args)";
    }
}