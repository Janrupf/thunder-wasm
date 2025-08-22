package net.janrupf.thunderwasm.runtime.linker;

import net.janrupf.thunderwasm.ThunderWasmException;
import net.janrupf.thunderwasm.data.Limits;
import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;
import net.janrupf.thunderwasm.runtime.linker.global.LinkedGlobalBase;
import net.janrupf.thunderwasm.runtime.linker.memory.LinkedMemory;
import net.janrupf.thunderwasm.runtime.linker.table.LinkedTable;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;

import java.util.List;

/**
 * A runtime linker that delegates to a list of other linkers.
 * <p>
 * This is useful for combining multiple linkers into one, allowing for modular linking strategies.
 */
public class DelegatingRuntimeLinker implements ProbingRuntimeLinker {
    private final List<ProbingRuntimeLinker> delegates;

    public DelegatingRuntimeLinker(List<ProbingRuntimeLinker> delegates) {
        this.delegates = delegates;
    }

    @Override
    public LinkedGlobalBase tryLinkGlobal(String moduleName, String importName, ValueType type, boolean readOnly)
            throws ThunderWasmException {
        return tryForAll((l) -> l.tryLinkGlobal(moduleName, importName, type, readOnly));
    }

    @Override
    public <T> LinkedTable<T> tryLinkTable(String moduleName, String importName, ReferenceType type, Limits limits) throws ThunderWasmException {
        return tryForAll((l) -> l.tryLinkTable(moduleName, importName, type, limits));
    }

    @Override
    public LinkedMemory tryLinkMemory(String moduleName, String importName, Limits limits) throws ThunderWasmException {
        return tryForAll((l) -> l.tryLinkMemory(moduleName, importName, limits));
    }

    @Override
    public LinkedFunction tryLinkFunction(String moduleName, String importName, FunctionType type) throws ThunderWasmException {
        return tryForAll((l) -> l.tryLinkFunction(moduleName, importName, type));
    }

    private <T> T tryForAll(TryLinkCallback<T> callback) throws ThunderWasmException {
        for (ProbingRuntimeLinker linker : delegates) {
            T result = callback.tryLink(linker);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private interface TryLinkCallback<T> {
        T tryLink(ProbingRuntimeLinker linker) throws ThunderWasmException;
    }
}
