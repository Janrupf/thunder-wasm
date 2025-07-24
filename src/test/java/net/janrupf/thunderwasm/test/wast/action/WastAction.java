package net.janrupf.thunderwasm.test.wast.action;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base class for WASM test actions that can be executed against a module.
 * Actions represent operations like function invocations or global variable access.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = InvokeAction.class, name = "invoke"),
    @JsonSubTypes.Type(value = GetAction.class, name = "get")
})
public abstract class WastAction {
    private final String actionName;
    private final String module;

    protected WastAction(String actionName, String module) {
        this.actionName = actionName;
        this.module = module;
    }
    
    /**
     * Gets the field/name being accessed by this action.
     * For invoke actions, this is the function name.
     * For get actions, this is the global variable name.
     *
     * @return the field name
     */
    public abstract String getField();

    /**
     * Retrieves the module name this action targets.
     *
     * @return the module name targeted, or null, if the current module is the target
     */
    public final String getModule() {
        return module;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(actionName).append(" ");

        if (module != null) {
            builder.append(module);
        } else {
            builder.append("<current>");
        }

        return builder.append(":").append(getField()).toString();
    }
}