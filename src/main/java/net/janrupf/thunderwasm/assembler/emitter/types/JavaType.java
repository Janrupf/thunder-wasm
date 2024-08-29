package net.janrupf.thunderwasm.assembler.emitter.types;

public abstract class JavaType {
    /**
     * Convert this type to a JVM descriptor.
     *
     * @return the JVM descriptor
     */
    public abstract String toJvmDescriptor();

    /**
     * Retrieves the number of slots this type occupies on the stack.
     *
     * @return the number of slots
     */
    public abstract int getSlotCount();

    @Override
    public String toString() {
        return toJvmDescriptor();
    }
}
