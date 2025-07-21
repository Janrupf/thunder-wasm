package net.janrupf.thunderwasm.assembler.emitter.frame;

interface JavaLocalSlot {
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
        public String toString() {
            return "<used " + local.toString() + ">";
        }
    }

    /**
     * A local slot that is currently not in sure, but has other locals stacked above it.
     */
    final class Vacant implements JavaLocalSlot {
        public static Vacant INSTANCE = new Vacant();

        @Override
        public String toString() {
            return "<vacant local>";
        }
    }

    /**
     * A local that is being occupied by the slot below.
     */
    final class Continuation implements JavaLocalSlot {
        public static Continuation INSTANCE = new Continuation();

        @Override
        public String toString() {
            return "<continuation local>";
        }
    }
}
