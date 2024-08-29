package net.janrupf.thunderwasm.module.section.segment;

import net.janrupf.thunderwasm.instructions.Expr;

public interface DataSegmentMode {
    /**
     * An active data segment.
     */
    final class Active implements DataSegmentMode {
        private final int memoryIndex;
        private final Expr memoryOffset;

        public Active(int memoryIndex, Expr memoryOffset) {
            this.memoryIndex = memoryIndex;
            this.memoryOffset = memoryOffset;
        }

        /**
         * Retrieves the index of the memory this data segment is targeting.
         *
         * @return the memory index
         */
        public int getMemoryIndex() {
            return memoryIndex;
        }

        /**
         * Retrieves the offset in the memory this data segment is targeting.
         *
         * @return the memory offset
         */
        public Expr getMemoryOffset() {
            return memoryOffset;
        }
    }

    /**
     * A passive data segment.
     */
    final class Passive implements DataSegmentMode {
        public static final Passive INSTANCE = new Passive();

        private Passive() {
        }
    }
}
