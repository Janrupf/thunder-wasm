package net.janrupf.thunderwasm.test.wast.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Command for registering a module instance with a specific name.
 * Used to make module instances available for import by other modules.
 */
public final class RegisterCommand extends WastCommand {
    private final String name;
    private final String as;

    @JsonCreator
    public RegisterCommand(@JsonProperty("line") int line, 
                          @JsonProperty("name") String name, 
                          @JsonProperty("as") String as) {
        super(line);
        this.name = name;
        this.as = as;
    }

    /**
     * Gets the module name to register.
     *
     * @return the module name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the name to register the module instance as.
     *
     * @return the registration name
     */
    public String getAs() {
        return as;
    }
}