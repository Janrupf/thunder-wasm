package net.janrupf.thunderwasm.runtime.linker;

import net.janrupf.thunderwasm.ThunderWasmException;
import net.janrupf.thunderwasm.data.Limits;
import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;
import net.janrupf.thunderwasm.runtime.linker.global.*;
import net.janrupf.thunderwasm.runtime.linker.memory.LinkedMemory;
import net.janrupf.thunderwasm.runtime.linker.table.LinkedTable;
import net.janrupf.thunderwasm.types.FunctionType;
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
    default LinkedGlobalBase linkGlobal(String moduleName, String importName, ValueType type, boolean readOnly)
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
    default <T> LinkedTable<T> linkTable(
            String moduleName,
            String importName,
            ReferenceType type,
            Limits limits
    )
            throws ThunderWasmException {
        throw new ThunderWasmException("Linkage of tables is not implemented in this runtime linker");
    }

    /**
     * Link a memory to a memory reference.
     *
     * @param moduleName the module name of the import
     * @param importName the import name of the import
     * @param limits     the memory limits
     * @return the linked memory
     * @throws ThunderWasmException if the memory could not be linked
     */
    default LinkedMemory linkMemory(String moduleName, String importName, Limits limits)
            throws ThunderWasmException {
        throw new ThunderWasmException("Linkage of memories is not implemented in this runtime linker");
    }

    /**
     * Link a function to a function reference.
     *
     * @param moduleName the module name of the import
     * @param importName the import name of the import
     * @param type       the function type
     * @return the linked function
     * @throws ThunderWasmException if the function could not be linked
     */
    default LinkedFunction linkFunction(String moduleName, String importName, FunctionType type)
            throws ThunderWasmException {
        throw new ThunderWasmException("Linkage of function is not implemented in this runtime linker");
    }

    /**
     * A runtime linker which doesn't provide any imports.
     */
    class Empty implements RuntimeLinker {
        public Empty() {}
    }
}
