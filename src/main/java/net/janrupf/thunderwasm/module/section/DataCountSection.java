package net.janrupf.thunderwasm.module.section;

public class DataCountSection extends WasmSection {
    private final int count;

    public DataCountSection(byte id, int count) {
        super(id);
        this.count = count;
    }

    /**
     * Retrieves the count of the data segments.
     *
     * @return the count of the data segments
     */
    public int getCount() {
        return count;
    }
}
