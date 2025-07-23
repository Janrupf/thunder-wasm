package net.janrupf.thunderwasm.lookup;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.data.Global;
import net.janrupf.thunderwasm.imports.*;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.*;
import net.janrupf.thunderwasm.module.section.segment.DataSegment;
import net.janrupf.thunderwasm.module.section.segment.ElementSegment;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.MemoryType;
import net.janrupf.thunderwasm.types.TableType;

public final class ElementLookups {
    private final ModuleLookups moduleLookups;

    public ElementLookups(ModuleLookups moduleLookups) {
        this.moduleLookups = moduleLookups;
    }

    /**
     * Require the global at the given index.
     *
     * @param i the index of the global
     * @return the found global
     * @throws WasmAssemblerException if the global could not be found
     */
    public FoundElement<Global, GlobalImportDescription> requireGlobal(LargeArrayIndex i) throws WasmAssemblerException {
        ImportSearchResult<GlobalImportDescription> res = findNthImportDescription(GlobalImportDescription.class, i);
        if (res.wasFound()) {
            return FoundElement.ofImport(
                    res.getImport(),
                    res.getNewSearchIndex()
            );
        }

        LargeArrayIndex globalSectionIndex = res.getNewSearchIndex();
        GlobalSection globalSection = moduleLookups.findSingleSection(GlobalSection.LOCATOR);
        if (globalSection == null || !globalSection.getGlobals().isValid(globalSectionIndex)) {
            throw new WasmAssemblerException("Global index " + i + " out of bounds");
        }

        return FoundElement.ofInternal(
                globalSection.getGlobals().get(globalSectionIndex),
                globalSectionIndex
        );
    }

    /**
     * Require the table at the given index.
     *
     * @param i the index of the table
     * @return the found table
     * @throws WasmAssemblerException if the table could not be found
     */
    public FoundElement<TableType, TableImportDescription> requireTable(LargeArrayIndex i) throws
            WasmAssemblerException {
        ImportSearchResult<TableImportDescription> res = findNthImportDescription(TableImportDescription.class, i);
        if (res.wasFound()) {
            return FoundElement.ofImport(
                    res.getImport(),
                    res.getNewSearchIndex()
            );
        }

        LargeArrayIndex tableSectionIndex = res.getNewSearchIndex();
        TableSection tableSection = moduleLookups.findSingleSection(TableSection.LOCATOR);
        if (tableSection == null || !tableSection.getTypes().isValid(tableSectionIndex)) {
            throw new WasmAssemblerException("Table index " + i + " out of bounds");
        }

        return FoundElement.ofInternal(
                tableSection.getTypes().get(tableSectionIndex),
                tableSectionIndex
        );
    }

    /**
     * Require the memory at the given index.
     *
     * @param i the index of the table
     * @return the found table
     * @throws WasmAssemblerException if the table could not be found
     */
    public FoundElement<MemoryType, MemoryImportDescription> requireMemory(LargeArrayIndex i) throws
            WasmAssemblerException {
        ImportSearchResult<MemoryImportDescription> res = findNthImportDescription(MemoryImportDescription.class, i);
        if (res.wasFound()) {
            return FoundElement.ofImport(
                    res.getImport(),
                    res.getNewSearchIndex()
            );
        }

        LargeArrayIndex memorySectionIndex = res.getNewSearchIndex();
        MemorySection memorySection = moduleLookups.findSingleSection(MemorySection.LOCATOR);
        if (memorySection == null || !memorySection.getTypes().isValid(memorySectionIndex)) {
            throw new WasmAssemblerException("Memory index " + i + " out of bounds");
        }

        return FoundElement.ofInternal(
                memorySection.getTypes().get(memorySectionIndex),
                memorySectionIndex
        );
    }

    /**
     * Require the element segment at the given index.
     *
     * @param i the index of the element segment
     * @return the found element segment
     * @throws WasmAssemblerException if the element segment could not be found
     */
    public ElementSegment requireElementSegment(LargeArrayIndex i) throws WasmAssemblerException {
        ElementSection elementSection = moduleLookups.findSingleSection(ElementSection.LOCATOR);
        if (elementSection == null || !elementSection.getSegments().isValid(i)) {
            throw new WasmAssemblerException("Element segment index " + i + " out of bounds");
        }

        return elementSection.getSegments().get(i);
    }

    /**
     * Require the data segment at the given index.
     *
     * @param i the index of the data segment
     * @return the found data segment
     * @throws WasmAssemblerException if the data segment could not be found
     */
    public DataSegment requireDataSegment(LargeArrayIndex i) throws WasmAssemblerException {
        DataSection dataSection = moduleLookups.findSingleSection(DataSection.LOCATOR);
        if (dataSection == null || !dataSection.getSegments().isValid(i)) {
            throw new WasmAssemblerException("Data segment index " + i + " out of bounds");
        }

        return dataSection.getSegments().get(i);
    }

    /**
     * Require the function type index at the given index.
     *
     * @param i the index of the function
     * @return the found function type index
     * @throws WasmAssemblerException if the function type could not be found
     */
    public int requireFunctionTypeIndex(LargeArrayIndex i) throws WasmAssemblerException {
        FunctionSection typeSection = moduleLookups.findSingleSection(FunctionSection.LOCATOR);
        if (typeSection == null || !typeSection.getTypes().isValid(i)) {
            throw new WasmAssemblerException("Function section index " + i + " out of bounds");
        }

        return typeSection.getTypes().get(i);
    }

    /**
     * Require the type at the given index.
     *
     * @param i the index of the type
     * @return the found type
     * @throws WasmAssemblerException if the type could not be found
     */
    public FunctionType requireType(LargeArrayIndex i) throws WasmAssemblerException {
        TypeSection typeSection = moduleLookups.findSingleSection(TypeSection.LOCATOR);
        if (typeSection == null || !typeSection.getTypes().isValid(i)) {
            throw new WasmAssemblerException("Type section index " + i + " out of bounds");
        }

        return typeSection.getTypes().get(i);
    }

    /**
     * Find the nth import description of the given type.
     *
     * @param type   the type of the import description
     * @param target the index of the import description to find
     * @param <T>    the type of the import description
     * @return the search result
     * @throws WasmAssemblerException if an error occurs while searching for the import description
     */
    private <T extends ImportDescription> ImportSearchResult<T> findNthImportDescription(
            Class<T> type,
            LargeArrayIndex target
    ) throws WasmAssemblerException {
        ImportSection importSection = moduleLookups.findSingleSection(ImportSection.LOCATOR);
        if (importSection != null) {
            LargeArrayIndex elementIndex = LargeArrayIndex.ZERO;

            for (Import<?> importEntry : importSection.getImports()) {
                Import<T> casted = importEntry.tryCast(type);

                if (casted != null) {
                    if (elementIndex.equals(target)) {
                        return ImportSearchResult.found(casted);
                    }

                    elementIndex = elementIndex.add(1);
                }
            }

            return ImportSearchResult.notFound(target.subtract(elementIndex));
        }

        return ImportSearchResult.notFound(target);
    }

    /**
     * The result of searching for an import.
     *
     * @param <T> the type of the import description
     */
    private static final class ImportSearchResult<T extends ImportDescription> {
        private final Import<T> im;
        private final LargeArrayIndex newSearchIndex;

        private ImportSearchResult(Import<T> im, LargeArrayIndex newSearchIndex) {
            this.im = im;
            this.newSearchIndex = newSearchIndex;
        }

        /**
         * Whether the import was found.
         *
         * @return true if the import was found, false otherwise
         */
        public boolean wasFound() {
            return im != null;
        }

        /**
         * Retrieves the found import.
         *
         * @return the import
         */
        public Import<T> getImport() {
            return im;
        }

        /**
         * Retrieves the new search index.
         *
         * @return the new search index
         */
        public LargeArrayIndex getNewSearchIndex() {
            return newSearchIndex;
        }

        /**
         * Creates a new search result for a found import.
         *
         * @param im  the found import
         * @param <T> the type of the import description
         * @return the created search result
         */
        public static <T extends ImportDescription> ImportSearchResult<T> found(Import<T> im) {
            return new ImportSearchResult<>(im, null);
        }

        /**
         * Creates a new search result for a not found import.
         *
         * @param newSearchIndex the new search index
         * @param <T>            the type of the import description
         * @return the created search result
         */
        public static <T extends ImportDescription> ImportSearchResult<T> notFound(LargeArrayIndex newSearchIndex) {
            return new ImportSearchResult<>(null, newSearchIndex);
        }
    }
}
