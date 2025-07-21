package net.janrupf.thunderwasm.runtime.linker.memory;

import net.janrupf.thunderwasm.data.Limits;

import java.nio.ByteBuffer;

public interface LinkedMemory {
    int PAGE_SIZE = 64 * 1024;

    /**
     * Get a reference to this memory as a byte buffer.
     * <p>
     * This will be called every time there has been a mutating change to
     * the memory buffer, however, for performance reasons, this will <b>not</b>
     * be called on every memory access.
     * <p>
     * Mutating changes are marked as such in this interface.
     *
     * @return the current backing byte buffer
     */
    ByteBuffer asInternal();

    /**
     * Grow the memory by a given amount of pages.
     * <p>
     * This is a mutating action and thus allowed to invalidate the underlying
     * buffer, if, and only if, successful. {@link #asInternal()} must be called
     * again to retrieve a new buffer instance, if growing succeeded.
     *
     * @param pages the number of pages to grow the buffer by.
     * @return true if growing the memory was successful, false otherwise
     */
    boolean grow(int pages);

    /**
     * Simple implementation of linked memory.
     */
    class Simple implements LinkedMemory {
        private ByteBuffer byteBuffer;
        private final Integer maxPages;

        public Simple(Limits limits) {
            this.byteBuffer = ByteBuffer.allocateDirect(PAGE_SIZE * limits.getMin());
            this.maxPages = limits.getMax();
        }

        @Override
        public ByteBuffer asInternal() {
            return byteBuffer;
        }

        @Override
        public boolean grow(int pages) {
            int currentPageCount = currentSize();
            int newPageCount = currentPageCount + pages;

            if (maxPages != null && newPageCount > maxPages) {
                return false;
            }

            // NOTE: The clear() here is confusing naming, it doesn't clear the buffer data, just resets
            // all possible changes to position, mark and limits
            ByteBuffer oldBuffer = this.byteBuffer.clear();
            this.byteBuffer = ByteBuffer.allocateDirect(PAGE_SIZE * newPageCount);
            this.byteBuffer.put(oldBuffer);

            return true;
        }

        public int currentSize() {
            return byteBuffer.capacity() / PAGE_SIZE;
        }
    }
}
