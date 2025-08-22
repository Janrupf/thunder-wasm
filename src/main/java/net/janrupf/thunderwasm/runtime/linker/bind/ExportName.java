package net.janrupf.thunderwasm.runtime.linker.bind;

import java.util.Objects;

/**
 * Represents a name of an export in a module.
 * <p>
 * This is used as a hash key for exports.
 */
public final class ExportName {
    private final String moduleName;
    private final String exportName;

    public ExportName(String moduleName, String exportName) {
        this.moduleName = moduleName;
        this.exportName = exportName;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ExportName)) return false;
        ExportName that = (ExportName) o;
        return Objects.equals(moduleName, that.moduleName) && Objects.equals(exportName, that.exportName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleName, exportName);
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getExportName() {
        return exportName;
    }

    @Override
    public String toString() {
        return moduleName + "@" + exportName;
    }
}
