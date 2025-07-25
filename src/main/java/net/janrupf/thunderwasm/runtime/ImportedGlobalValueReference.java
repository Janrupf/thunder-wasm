package net.janrupf.thunderwasm.runtime;

import net.janrupf.thunderwasm.imports.GlobalImportDescription;
import net.janrupf.thunderwasm.imports.Import;

/**
 * This is used for delay initializing global based on imported globals.
 */
public final class ImportedGlobalValueReference {
    private final Import<GlobalImportDescription> importDescription;

    public ImportedGlobalValueReference(Import<GlobalImportDescription> importDescription) {
        this.importDescription = importDescription;
    }

    /**
     * Retrieve the import referenced by this value.
     *
     * @return the import referenced by this value
     */
    public Import<GlobalImportDescription> getImportDescription() {
        return importDescription;
    }
}
