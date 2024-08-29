package net.janrupf.thunderwasm.assembler;

import net.janrupf.thunderwasm.ThunderWasmException;

public class WasmAssemblerException extends ThunderWasmException {
    public WasmAssemblerException(String message) {
        super(message);
    }

    public WasmAssemblerException(String message, Throwable cause) {
        super(message, cause);
    }
}
