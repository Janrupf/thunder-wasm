package net.janrupf.thunderwasm.assembler.emitter.frame;

import java.util.Objects;

public interface JavaLocalSlot {
    /**
     * Create a vacant local slot.
     *
     * @return the created slot
     */
    static Vacant vacant() {
        return Vacant.INSTANCE;
    }

    /**
     * Create a used local slot.
     *
     * @param local the local occupying the slot
     * @return the created slot
     */
    static Used used(JavaLocal local) {
        return new Used(local);
    }

    /**
     * Create a continuation slot.
     *
     * @return the created slot
     */
    static Continuation continuation() {
        return Continuation.INSTANCE;
    }

    final class Used implements JavaLocalSlot {
        private final JavaLocal local;

        private Used(JavaLocal local) {
            this.local = local;
        }

        /**
         * Retrieves local in this slot
         *
         * @return the local in this slot
         */
        public JavaLocal getLocal() {
            return local;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Used)) return false;
            Used used = (Used) o;
            return Objects.equals(local, used.local);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(local);
        }

        @Override
        public String toString() {
            return "<used " + local.toString() + ">";
        }
    }

    /**
     * A local slot that is currently not in sure, but has other locals stacked above it.
     */
    final class Vacant implements JavaLocalSlot {
        public static final Vacant INSTANCE = new Vacant();

        private Vacant() {}

        @SuppressWarnings("EqualsDoesntCheckParameterClass")
        @Override
        public boolean equals(Object obj) {
            return obj == INSTANCE;
        }

        @Override
        public String toString() {
            return "<vacant local>";
        }
    }

    /**
     * A local that is being occupied by the slot below.
     */
    final class Continuation implements JavaLocalSlot {
        public static final Continuation INSTANCE = new Continuation();

        private Continuation() {}

        @SuppressWarnings("EqualsDoesntCheckParameterClass")
        @Override
        public boolean equals(Object obj) {
            return obj == INSTANCE;
        }

        @Override
        public String toString() {
            return "<continuation local>";
        }
    }
}
