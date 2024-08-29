package net.janrupf.thunderwasm;

public class ThunderWasmException extends Exception {
    public ThunderWasmException(String message) {
        super(message);
    }

    public ThunderWasmException(String message, Throwable cause) {
        super(message, cause);
    }
}
