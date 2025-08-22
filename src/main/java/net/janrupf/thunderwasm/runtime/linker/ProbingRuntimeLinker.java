package net.janrupf.thunderwasm.runtime.linker;

import net.janrupf.thunderwasm.ThunderWasmException;
import net.janrupf.thunderwasm.data.Limits;
import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;
import net.janrupf.thunderwasm.runtime.linker.global.LinkedGlobal;
import net.janrupf.thunderwasm.runtime.linker.global.LinkedGlobalBase;
import net.janrupf.thunderwasm.runtime.linker.memory.LinkedMemory;
import net.janrupf.thunderwasm.runtime.linker.table.LinkedTable;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;

/**
 * A runtime linker that doesn't always throw when trying to link an import that does not exist.
 */
public interface ProbingRuntimeLinker extends RuntimeLinker {
    /**
     * Attempt to link a global to a global reference.
     * <p>
     * This functions needs to return an appropriate subclass of {@link LinkedGlobal} based on the type of the global.
     *
     * @param moduleName the module name of the import
     * @param importName the import name of the import
     * @param type       the type of the global
     * @param readOnly   whether the global is read-only
     * @return the linked global, or null if the global could not be found
     * @throws ThunderWasmException if the global is of a wrong type or another error occurs
     */
    default LinkedGlobalBase tryLinkGlobal(String moduleName, String importName, ValueType type, boolean readOnly)
            throws ThunderWasmException {
        return null;
    }

    @Override
    default LinkedGlobalBase linkGlobal(String moduleName, String importName, ValueType type, boolean readOnly) throws ThunderWasmException {
        return ensureFound(
                tryLinkGlobal(moduleName, importName, type, readOnly),
                "Global",
                moduleName,
                importName
        );
    }

    /**
     * Attempt to link a table to a table reference.
     *
     * @param moduleName the module name of the import
     * @param importName the import name of the import
     * @param type       the type of the values stored in the table
     * @param limits     the table limits
     * @param <T>        the table value type
     * @return the linked table, or null if the table could not be found
     * @throws ThunderWasmException if the table is of a wrong type or another error occurs
     */
    default <T> LinkedTable<T> tryLinkTable(
            String moduleName,
            String importName,
            ReferenceType type,
            Limits limits
    ) throws ThunderWasmException {
        return null;
    }

    @Override
    default <T> LinkedTable<T> linkTable(String moduleName, String importName, ReferenceType type, Limits limits)
            throws ThunderWasmException {
        return ensureFound(
                tryLinkTable(moduleName, importName, type, limits),
                "Table",
                moduleName,
                importName
        );
    }

    /**
     * Attempt to link a memory to a memory reference.
     *
     * @param moduleName the module name of the import
     * @param importName the import name of the import
     * @param limits     the memory limits
     * @return the linked memory, or null if the memory could not be found
     * @throws ThunderWasmException if the table is of a wrong type or another error occurs
     */
    default LinkedMemory tryLinkMemory(String moduleName, String importName, Limits limits)
            throws ThunderWasmException {
        return null;
    }

    @Override
    default LinkedMemory linkMemory(String moduleName, String importName, Limits limits) throws ThunderWasmException {
        return ensureFound(
                tryLinkMemory(moduleName, importName, limits),
                "Memory",
                moduleName,
                importName
        );
    }

    /**
     * Attempt to link a function to a function reference.
     *
     * @param moduleName the module name of the import
     * @param importName the import name of the import
     * @param type       the function type
     * @return the linked function, or null if the function could not be found
     * @throws ThunderWasmException if the function is of a wrong type or another error occurs
     */
    default LinkedFunction tryLinkFunction(String moduleName, String importName, FunctionType type)
            throws ThunderWasmException {
        return null;
    }

    @Override
    default LinkedFunction linkFunction(String moduleName, String importName, FunctionType type) throws ThunderWasmException {
        return ensureFound(
                tryLinkFunction(moduleName, importName, type),
                "Function",
                moduleName,
                importName
        );
    }

    static <T> T ensureFound(T element, String t, String moduleName, String importName)
            throws ThunderWasmException {
        if (element == null) {
            throw new ThunderWasmException(t + " '" + moduleName + "@" + importName + "' not found");
        }

        return element;
    }
}
