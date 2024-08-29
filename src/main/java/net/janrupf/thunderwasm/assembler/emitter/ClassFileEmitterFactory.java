package net.janrupf.thunderwasm.assembler.emitter;

/**
 * Top level abstraction for emitting class files.
 */
public interface ClassFileEmitterFactory {
    /**
     * Creates a new {@link ClassFileEmitter} for the given package and class name.
     *
     * @param packageName the package name
     * @param className the class name
     * @return the created {@link ClassFileEmitter}
     */
    ClassFileEmitter createFor(String packageName, String className);
}
