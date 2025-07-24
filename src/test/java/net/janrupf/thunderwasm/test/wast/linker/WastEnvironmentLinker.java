package net.janrupf.thunderwasm.test.wast.linker;

import net.janrupf.thunderwasm.ThunderWasmException;
import net.janrupf.thunderwasm.data.Limits;
import net.janrupf.thunderwasm.runtime.ElementReference;
import net.janrupf.thunderwasm.runtime.WasmModuleExports;
import net.janrupf.thunderwasm.runtime.linker.RuntimeLinker;
import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;
import net.janrupf.thunderwasm.runtime.linker.global.LinkedGlobal;
import net.janrupf.thunderwasm.runtime.linker.global.LinkedGlobalBase;
import net.janrupf.thunderwasm.runtime.linker.global.LinkedReadOnlyGlobal;
import net.janrupf.thunderwasm.runtime.linker.memory.LinkedMemory;
import net.janrupf.thunderwasm.runtime.linker.table.LinkedTable;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;

import java.util.Map;

public class WastEnvironmentLinker implements RuntimeLinker {
    private final SpectestEnvironment environment;
    private final Map<String, Object> registeredModules;

    public WastEnvironmentLinker(SpectestEnvironment environment, Map<String, Object> registeredModules) {
        this.environment = environment;
        this.registeredModules = registeredModules;
    }

    @Override
    public LinkedFunction linkFunction(String moduleName, String importName, FunctionType type) throws ThunderWasmException {
        if (moduleName.equals("spectest")) {
            return environment.getFunction(importName, type);
        }

        LinkedFunction exported = requireExport(LinkedFunction.class, moduleName, importName);

        if (!exported.getArguments().equals(type.getInputs().asFlatList())) {
            throw new LinkageFailedException("incompatible import type");
        }

        if (!exported.getReturnTypes().equals(type.getOutputs().asFlatList())) {
            throw new LinkageFailedException("incompatible import type");
        }

        return exported;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ElementReference> LinkedTable<T> linkTable(
            String moduleName,
            String importName,
            ReferenceType type,
            Limits limits
    ) throws ThunderWasmException {
        if (moduleName.equals("spectest")) {
            return (LinkedTable<T>) environment.getTable(importName, type);
        }

        return (LinkedTable<T>) requireExport(LinkedTable.class, moduleName, importName);
    }

    @Override
    public LinkedGlobalBase linkGlobal(String moduleName, String importName, ValueType type, boolean readOnly) throws ThunderWasmException {
        if (moduleName.equals("spectest")) {
            return environment.getGlobal(importName, type, readOnly);
        }

        LinkedReadOnlyGlobal exported = requireExport(LinkedReadOnlyGlobal.class, moduleName, importName);

        if (!exported.getType().equals(type)) {
            throw new LinkageFailedException("incompatible import type");
        }

        if (!readOnly && !(exported instanceof LinkedGlobal)) {
            throw new LinkageFailedException("incompatible import type");
        }

        return exported;
    }

    @Override
    public LinkedMemory linkMemory(String moduleName, String importName, Limits limits) throws ThunderWasmException {
        if (moduleName.equals("spectest")) {
            return environment.getMemory(importName);
        }

        return requireExport(LinkedMemory.class, moduleName, importName);
    }

    private <T> T requireExport(Class<T> clazz, String moduleName, String importName) throws LinkageFailedException {
        if (!registeredModules.containsKey(moduleName)) {
            throw new LinkageFailedException("unknown import");
        }

        Map<String, Object> e = ((WasmModuleExports) registeredModules.get(moduleName)).getExports();
        if (!e.containsKey(importName)) {
            throw new LinkageFailedException("unknown import");
        }

        Object export = e.get(importName);

        try {
            return clazz.cast(export);
        } catch (ClassCastException ex) {
            throw new LinkageFailedException("incompatible import type", ex);
        }
    }

    public static class LinkageFailedException extends ThunderWasmException {
        public LinkageFailedException(String message) {
            super(message);
        }

        public LinkageFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
