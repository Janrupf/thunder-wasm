package net.janrupf.thunderwasm.lookup;

import net.janrupf.thunderwasm.module.section.WasmSection;

/**
 * Helper to locate sections.
 */
public interface SectionLocator<T extends WasmSection> {
    /**
     * Retrieves the type of the section that is being located.
     *
     * @return the section type
     */
    Class<T> getSectionType();

    /**
     * Retrieves the section id of the section that is being located.
     *
     * @return the section id
     */
    byte getSectionId();

    /**
     * Create a new section locator for the given section id.
     *
     * @param type the section type
     * @param id   the section id
     * @param <T>  the type of the section
     * @return the section locator
     */
    static <T extends WasmSection> SectionLocator<T> of(Class<T> type, byte id) {
        return new SectionLocator<T>() {
            @Override
            public Class<T> getSectionType() {
                return type;
            }

            @Override
            public byte getSectionId() {
                return id;
            }
        };
    }
}
