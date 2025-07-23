package net.janrupf.thunderwasm.assembler;

import net.janrupf.thunderwasm.imports.Import;
import net.janrupf.thunderwasm.imports.TypeImportDescription;
import net.janrupf.thunderwasm.lookup.ModuleLookups;
import net.janrupf.thunderwasm.module.section.CodeSection;
import net.janrupf.thunderwasm.module.section.ImportSection;

public final class WasmAssemblerStatistics {
    private final int localFunctionCount;
    private final int importedFunctionCount;
    private final int totalFunctionCount;

    private WasmAssemblerStatistics(int localFunctionCount, int importedFunctionCount, int totalFunctionCount) {
        this.localFunctionCount = localFunctionCount;
        this.importedFunctionCount = importedFunctionCount;
        this.totalFunctionCount = totalFunctionCount;
    }

    /**
     * Retrieve the local function count.
     *
     * @return the number of local functions
     */
    public int getLocalFunctionCount() {
        return localFunctionCount;
    }

    /**
     * Retrieve the imported function count.
     *
     * @return the number of imported functions
     */
    public int getImportedFunctionCount() {
        return importedFunctionCount;
    }

    /**
     * Retrieve the total function count.
     *
     * @return the total number of functions (local + imported)
     */
    public int getTotalFunctionCount() {
        return totalFunctionCount;
    }

    /**
     * Calculate the statistics for the given module lookups and element lookups.
     *
     * @param lookups the module lookups
     * @return the calculated statistics
     * @throws WasmAssemblerException if an error occurs during calculation
     */
    public static WasmAssemblerStatistics calculate(
            ModuleLookups lookups
    ) throws WasmAssemblerException {
        CodeSection codeSection = lookups.requireSingleSection(CodeSection.LOCATOR);
        ImportSection importSection = lookups.findSingleSection(ImportSection.LOCATOR);

        if (codeSection.getFunctions().length() > Integer.MAX_VALUE) {
            throw new WasmAssemblerException("Too many functions in code section, cannot handle more than " + Integer.MAX_VALUE);
        }

        int localFunctionCount = (int) codeSection.getFunctions().length();

        int importedFunctionCount = 0;
        if (importSection != null) {
            for (Import<?> im : importSection.getImports()) {
                if (im.tryCast(TypeImportDescription.class) != null) {
                    importedFunctionCount++;
                }
            }
        }

        int totalFunctionCount = localFunctionCount + importedFunctionCount;
        if (totalFunctionCount < 0 /* overflow */) {
            throw new WasmAssemblerException(
                    "Too many functions in code combined with imports, cannot handle more than " + Integer.MAX_VALUE +
                            " total functions");
        }

        return new WasmAssemblerStatistics(
                localFunctionCount,
                importedFunctionCount,
                totalFunctionCount
        );
    }
}
