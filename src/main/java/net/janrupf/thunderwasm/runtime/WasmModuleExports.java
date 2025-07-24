package net.janrupf.thunderwasm.runtime;

import java.util.Map;

public interface WasmModuleExports {
    /**
     * Retrieve all the exports the module has.
     *
     * @return the module exports
     */
    Map<String, Object> getExports();
}
