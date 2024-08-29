package net.janrupf.thunderwasm.module;

import net.janrupf.thunderwasm.ThunderWasmException;

public class InvalidModuleException extends ThunderWasmException {
    public InvalidModuleException(String message) {
        super(message);
    }

    public InvalidModuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
