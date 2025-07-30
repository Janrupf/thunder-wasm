package net.janrupf.thunderwasm.assembler.analysis;

import java.util.HashMap;
import java.util.Map;

/**
 * Analysis container of local variables.
 */
public final class LocalVariableUsage {
    private final LocalVariableUsage parent;
    private final Map<Integer, Status> status;

    public LocalVariableUsage(LocalVariableUsage parent) {
        this.parent = parent;
        this.status = new HashMap<>();
    }

    /**
     * Mark a local variable as being read.
     *
     * @param id the id of the variable being read
     */
    public void read(int id) {
        if (this.parent != null) {
            this.parent.read(id);
        }

        this.use(id).read = true;
    }

    /**
     * Mark a local variable as being written.
     *
     * @param id the id of the variable being written
     */
    public void write(int id) {
        if (this.parent != null) {
            this.parent.write(id);
        }

        this.use(id).write = true;
    }

    /**
     * Retrieve the status of this usage.
     *
     * @return the status of this usage
     */
    public Map<Integer, Status> getStatus() {
        return this.status;
    }

    private Status use(int id) {
        return this.status.computeIfAbsent(id, (x) -> new Status());
    }

    public static final class Status {
        private boolean read;
        private boolean write;

        public Status() {
            this.read = false;
            this.write = false;
        }

        /**
         * Determine whether the local variable was read.
         *
         * @return true if the local variable was read, false otherwise
         */
        public boolean wasRead() {
            return read;
        }

        /**
         * Determine whether the local variable was written.
         *
         * @return true if the local variable was written, false otherwise
         */
        public boolean wasWritten() {
            return write;
        }
    }
}
