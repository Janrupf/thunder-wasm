package net.janrupf.thunderwasm.assembler;

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
     * @param sectionType the type of section to retrieve
     * @param id          the id of the section to retrieve
     * @param <T>         the type of section to retrieve
     * @return all sections of the given type
     * @throws WasmAssemblerException if the section types mismatch
     */
    public <T extends WasmSection> List<T> allSections(Class<T> sectionType, byte id)
            throws WasmAssemblerException {
        CachedSectionList cached = cache[id];

        if (cached == null) {
            return cacheSections(sectionType, id);
        } else {
            return cached.getAll(sectionType);
        }
    }

    /**
     * Retrieves a single section of the given type from the module.
     *
     * @param sectionType the type of section to retrieve
     * @param id          the id of the section to retrieve
     * @param <T>         the type of section to retrieve
     * @return the section of the given type
     * @throws WasmAssemblerException if the section types mismatch
     */
    public <T extends WasmSection> T requireSingleSection(Class<T> sectionType, byte id) throws WasmAssemblerException {
        CachedSectionList cached = cache[id];

        if (cached == null) {
            List<T> l = cacheSections(sectionType, id);
            if (l.size() != 1) {
                throw new WasmAssemblerException(
                        "Expected exactly one section of type " + sectionType.getName()
                                + " but got " + l.size()
                );
            }

            return l.get(0);
        } else {
            return cached.getSingle(sectionType);
        }
    }

    /**
     * Populates the cache with all sections of the given type.
     *
     * @param sectionType the type of section to cache
     * @param id          the id of the section to cache
     * @param <T>         the type of section to cache
     * @return the cached sections
     * @throws WasmAssemblerException if the section types mismatch
     */
    private <T extends WasmSection> List<T> cacheSections(Class<T> sectionType, byte id) throws WasmAssemblerException {
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
         * Extracts a single section of the given type from this list.
         *
         * @param targetType the type of section to extract
         * @param <T>        the type of section to extract
         * @return the extracted section
         * @throws WasmAssemblerException if there is not exactly one section of the given type
         */
        public <T extends WasmSection> T getSingle(Class<T> targetType) throws WasmAssemblerException {
            if (!canExtract(targetType)) {
                throw new WasmAssemblerException(
                        "Section id-type mismatch, got sections of type " + sectionType.getName()
                                + " but expected " + targetType.getName()
                );
            }

            if (sections.size() != 1) {
                throw new WasmAssemblerException(
                        "Expected exactly one section of type " + targetType.getName()
                                + " but got " + sections.size()
                );
            }

            return targetType.cast(sections.get(0));
        }
    }
}
