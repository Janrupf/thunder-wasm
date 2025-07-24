package net.janrupf.thunderwasm.test.wast;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.janrupf.thunderwasm.test.wast.command.WastCommand;

import java.util.List;

public final class WastManifest {
    private final String sourceFilename;
    private final List<WastCommand> commands;

    @JsonCreator
    public WastManifest(@JsonProperty("source_filename") String sourceFilename, @JsonProperty("commands") List<WastCommand> commands) {
        this.sourceFilename = sourceFilename;
        this.commands = commands;
    }

    public String getSourceFilename() {
        return sourceFilename;
    }

    public List<WastCommand> getCommands() {
        return commands;
    }
}
