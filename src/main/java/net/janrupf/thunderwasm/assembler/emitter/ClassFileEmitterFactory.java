package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;

import java.util.List;

/**
 * Top level abstraction for emitting class files.
 */
public interface ClassFileEmitterFactory {
    /**
     * Creates a new {@link ClassFileEmitter} for the given package and class name.
     *
     * @param packageName the package name
     * @param className the class name
     * @param superType the super type of the class
     * @param interfaceTypes the interface types to add
     * @return the created {@link ClassFileEmitter}
     */
    ClassFileEmitter createFor(
            String packageName,
            String className,
            ObjectType superType,
            List<ObjectType> interfaceTypes
    );
}
