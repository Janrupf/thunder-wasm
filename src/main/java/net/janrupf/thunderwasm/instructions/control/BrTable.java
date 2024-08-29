package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.encoding.LargeIntArray;

import java.io.IOException;

public final class BrTable extends WasmInstruction<BrTable.Data> {
    public static final BrTable INSTANCE = new BrTable();

    private BrTable() {
        super("br_table", (byte) 0x0E);
    }

    @Override
    public Data readData(WasmLoader loader) throws IOException, InvalidModuleException {
        LargeIntArray branchLabels = loader.readU32Vec();
        int defaultLabel = loader.readU32();

        return new Data(branchLabels, defaultLabel);
    }

    public static final class Data implements WasmInstruction.Data {
        private final LargeIntArray branchLabels;
        private final int defaultLabel;

        private Data(LargeIntArray branchLabels, int defaultLabel) {
            this.branchLabels = branchLabels;
            this.defaultLabel = defaultLabel;
        }

        /**
         * Retrieves the branch labels.
         *
         * @return the branch labels
         */
        public LargeIntArray getBranchLabels() {
            return branchLabels;
        }

        /**
         * Retrieves the default label.
         *
         * @return the default label
         */
        public int getDefaultLabel() {
            return defaultLabel;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("[");
            for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.toU64() < branchLabels.length(); i = i.add(1)) {
                if (i.compareTo(LargeArrayIndex.ZERO) > 0) {
                    builder.append(", ");
                }

                builder.append(i);
                builder.append(" -> ");
                builder.append(branchLabels.get(i));
            }

            if (branchLabels.length() > 0) {
                builder.append(", ");
            }

            builder.append("default -> ");
            builder.append(defaultLabel);
            builder.append("]");
            return builder.toString();
        }
    }
}
