package net.janrupf.thunderwasm.runtime.linker;

import net.janrupf.thunderwasm.ThunderWasmException;
import net.janrupf.thunderwasm.runtime.linker.global.*;
import net.janrupf.thunderwasm.types.ValueType;

public interface RuntimeLinker {
    /**
     * Link a global to a global reference.
     * <p>
     * This functions needs to return an appropriate subclass of {@link LinkedGlobal} based on the type of the global.
     *
     * @param moduleName the module name of the import
     * @param importName the import name of the import
     * @param type       the type of the global
     * @param readOnly   whether the global is read-only
     * @return the linked global
     * @throws ThunderWasmException if the global could not be linked
     */
    LinkedGlobal linkGlobal(String moduleName, String importName, ValueType type, boolean readOnly)
            throws ThunderWasmException;
}
