package net.janrupf.thunderwasm.runtime.linker.bind;

public class ExportDescription<T> {
    private final T export;
    private final ExportName name;
    private final boolean readOnly;
    private final WasmExport.Type type;

    public ExportDescription(T export, String moduleName, String exportName, boolean readOnly, WasmExport.Type type) {
        this.export = export;
        this.name = new ExportName(moduleName, exportName);
        this.readOnly = readOnly;
        this.type = type;
    }

    public T getExport() {
        return export;
    }

    public ExportName getName() {
        return name;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public WasmExport.Type getType() {
        return type;
    }
}
