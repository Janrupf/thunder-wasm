package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;

public interface ClassFileEmitter {
    MethodEmitter method(
            String methodName,
            Visibility visibility,
            boolean isStatic,
            boolean isFinal,
            JavaType returnType,
            JavaType[] parameterTypes,
            JavaType[] thrownTypes
    );

    /**
     * Finalizes the class file and returns the bytecode.
     *
     * @return the bytecode of the class file
     */
    byte[] finish();
}
