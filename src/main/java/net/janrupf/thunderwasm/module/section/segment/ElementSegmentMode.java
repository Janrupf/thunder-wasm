package net.janrupf.thunderwasm.module.section.segment;

import net.janrupf.thunderwasm.instructions.Expr;

public interface ElementSegmentMode {
    final class Active implements ElementSegmentMode {
        private final int tableIndex;
        private final Expr tableOffset;

        public Active(int tableIndex, Expr tableOffset) {
            this.tableIndex = tableIndex;
            this.tableOffset = tableOffset;
        }

        /**
         * Returns the index of the table that this element segment is targeting.
         *
         * @return the index of the table
         */
        public int getTableIndex() {
            return tableIndex;
        }

        /**
         * Returns the offset in the table that this element segment is targeting.
         *
         * @return the offset in the table
         */
        public Expr getTableOffset() {
            return tableOffset;
        }
    }

    /**
     * A passive element segment.
     */
    final class Passive implements ElementSegmentMode {
        public static final Passive INSTANCE = new Passive();

        private Passive() {
        }
    }

    /**
     * A declarative element segment.
     */
    final class Declarative implements ElementSegmentMode {
        public static final Declarative INSTANCE = new Declarative();

        private Declarative() {
        }
    }
}
