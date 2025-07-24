package net.janrupf.thunderwasm.test.wast.action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * WASM test action for getting the value of a global variable.
 * Represents global variable access operations in WAST test definitions.
 */
public final class GetAction extends WastAction {
    private final String field;
    
    @JsonCreator
    public GetAction(
            @JsonProperty("module") String module,
            @JsonProperty("field") String field) {
        super("get", module);
        this.field = field;
    }
    

    @Override
    public String getField() {
        return field;
    }
    
    /**
     * Gets the global variable name to access.
     *
     * @return the global variable name
     */
    public String getGlobalName() {
        return field;
    }
}
