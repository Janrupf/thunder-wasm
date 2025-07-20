package net.janrupf.thunderwasm.runtime.linker;

import net.janrupf.thunderwasm.ThunderWasmException;
import net.janrupf.thunderwasm.data.Limits;
import net.janrupf.thunderwasm.runtime.ElementReference;
import net.janrupf.thunderwasm.runtime.linker.global.*;
import net.janrupf.thunderwasm.runtime.linker.table.LinkedTable;
import net.janrupf.thunderwasm.types.ReferenceType;
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
    default LinkedGlobal linkGlobal(String moduleName, String importName, ValueType type, boolean readOnly)
            throws ThunderWasmException {
        throw new ThunderWasmException("Linkage of globals is not implemented in this runtime linker");
    }

    /**
     * Link a table to a table reference.
     *
     * @param moduleName the module name of the import
     * @param importName the import name of the import
     * @param type       the type of the values stored in the table
     * @param limits     the table limits
     * @param <T>        the table value type
     * @return the linked table
     * @throws ThunderWasmException if the table could not be linked
     */
    default <T extends ElementReference> LinkedTable<T> linkTable(
            String moduleName,
            String importName,
            ReferenceType type,
            Limits limits
    )
            throws ThunderWasmException {
        throw new ThunderWasmException("Linkage of tables is not implemented in this runtime linker");
    }
}
