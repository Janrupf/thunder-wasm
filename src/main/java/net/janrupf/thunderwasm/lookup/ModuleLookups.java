package net.janrupf.thunderwasm.lookup;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.module.section.WasmSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleLookups {
    private final WasmModule module;
    private final CachedSectionList[] cache;

    public ModuleLookups(WasmModule module) {
        this.module = module;
        this.cache = new CachedSectionList[256];
    }

    /**
     * Retrieves all sections of the given type from the module.
     *
     * @param locator the locator for the sections to retrieve
     * @param <T>     the type of section to retrieve
     * @return all sections of the given type
     * @throws WasmAssemblerException if the section types mismatch
     */
    public <T extends WasmSection> List<T> allSections(SectionLocator<T> locator)
            throws WasmAssemblerException {
        byte id = locator.getSectionId();
        Class<T> sectionType = locator.getSectionType();

        CachedSectionList cached = cache[id];

        if (cached == null) {
            return cacheSections(locator);
        } else {
            return cached.getAll(sectionType);
        }
    }

    /**
     * Retrieves a single section of the given type from the module.
     *
     * @param locator the locator for the section to retrieve
     * @param <T>     the type of section to retrieve
     * @return the section of the given type
     * @throws WasmAssemblerException if the section types mismatch or multiple sections are found
     */
    public <T extends WasmSection> T findSingleSection(SectionLocator<T> locator) throws WasmAssemblerException {
        byte id = locator.getSectionId();
        Class<T> sectionType = locator.getSectionType();

        CachedSectionList cached = cache[id];

        if (cached == null) {
            List<T> l = cacheSections(locator);
            if (l.size() > 1) {
                throw new WasmAssemblerException(
                        "Expected at most one section of type " + sectionType.getName()
                                + " but got " + l.size()
                );
            }

            return l.isEmpty() ? null : l.get(0);
        } else {
            return cached.findSingle(sectionType);
        }
    }

    /**
     * Retrieves a single section of the given type from the module.
     *
     * @param locator the locator for the section to retrieve
     * @param <T>     the type of section to retrieve
     * @return the section of the given type
     * @throws WasmAssemblerException if the section types mismatch
     */
    public <T extends WasmSection> T requireSingleSection(SectionLocator<T> locator) throws WasmAssemblerException {
        T value = findSingleSection(locator);

        if (value == null) {
            throw new WasmAssemblerException(
                    "Expected exactly one section of type " + locator.getSectionType().getName()
                            + " but got none"
            );
        }

        return value;
    }

    /**
     * Populates the cache with all sections of the given type.
     *
     * @param locator the locator for the section to cache
     * @param <T>     the type of section to cache
     * @return the cached sections
     * @throws WasmAssemblerException if the section types mismatch
     */
    private <T extends WasmSection> List<T> cacheSections(SectionLocator<T> locator) throws WasmAssemblerException {
        byte id = locator.getSectionId();
        Class<T> sectionType = locator.getSectionType();

        List<T> matchingSections = new ArrayList<>();

        for (WasmSection section : module.getSections()) {
            byte sectionId = section.getId();

            if (sectionId == id) {
                if (!sectionType.isAssignableFrom(section.getClass())) {
                    throw new WasmAssemblerException(
                            "Section id-type mismatch, got sections of type " + section.getClass().getName()
                                    + " but expected " + sectionType.getName()
                    );
                }

                matchingSections.add(sectionType.cast(section));
            }
        }

        List<T> frozen = Collections.unmodifiableList(matchingSections);
        cache[id] = new CachedSectionList(sectionType, frozen);

        return frozen;
    }

    private static final class CachedSectionList {
        private final Class<? extends WasmSection> sectionType;
        private final List<? extends WasmSection> sections;

        private CachedSectionList(Class<? extends WasmSection> sectionType, List<? extends WasmSection> sections) {
            this.sectionType = sectionType;
            this.sections = sections;
        }

        /**
         * Determines whether this list can be used to extract a section of the given type.
         *
         * @param targetType the type of section to extract
         * @return true if the section can be extracted, false otherwise
         */
        public boolean canExtract(Class<? extends WasmSection> targetType) {
            return targetType.isAssignableFrom(sectionType);
        }

        /**
         * Extracts a section of the given type from this list.
         *
         * @param targetType the type of section to extract
         * @return the extracted section
         * @throws WasmAssemblerException if the section types mismatch
         */
        @SuppressWarnings("unchecked")
        public <T extends WasmSection> List<T> getAll(Class<T> targetType) throws WasmAssemblerException {
            if (!canExtract(targetType)) {
                throw new WasmAssemblerException(
                        "Section id-type mismatch, got sections of type " + sectionType.getName()
                                + " but expected " + targetType.getName()
                );
            }

            return (List<T>) sections;
        }

        /**
         * Finds a single section of the given type from this list.
         *
         * @param targetType the type of section to extract
         * @param <T>        the type of section to extract
         * @return the extracted section
         * @throws WasmAssemblerException if there is not exactly one section of the given type
         */
        public <T extends WasmSection> T findSingle(Class<T> targetType) throws WasmAssemblerException {
            if (!canExtract(targetType)) {
                throw new WasmAssemblerException(
                        "Section id-type mismatch, got sections of type " + sectionType.getName()
                                + " but expected " + targetType.getName()
                );
            }

            if (sections.size() > 1) {
                throw new WasmAssemblerException(
                        "Expected at most one section of type " + targetType.getName()
                                + " but got " + sections.size()
                );
            }

            return sections.isEmpty() ? null : targetType.cast(sections.get(0));
        }

        /**
         * Requires a single section of the given type from this list.
         *
         * @param targetType the type of section to extract
         * @param <T>        the type of section to extract
         * @return the extracted section
         * @throws WasmAssemblerException if there is not exactly one section of the given type
         */
        public <T extends WasmSection> T requireSingle(Class<T> targetType) throws WasmAssemblerException {
            T value = findSingle(targetType);

            if (value == null) {
                throw new WasmAssemblerException(
                        "Expected exactly one section of type " + targetType.getName()
                                + " but got none"
                );
            }

            return value;
        }
    }
}
