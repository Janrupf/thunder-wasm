package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;

public interface ClassFileEmitter {
    /**
     * Retrieve the type of the class being emitted.
     *
     * @return the type of the class being emitted
     */
    ObjectType getOwner();

    /**
     * Add a field to the class file.
     *
     * @param fieldName  the name of the field
     * @param visibility the visibility of the field
     * @param isStatic   whether the field is static
     * @param isFinal    whether the field is final
     * @param type       the type of the field
     */
    void field(
            String fieldName,
            Visibility visibility,
            boolean isStatic,
            boolean isFinal,
            JavaType type
    );

    /**
     * Add a method to the class file.
     *
     * @param methodName     the name of the method
     * @param visibility     the visibility of the method
     * @param isStatic       whether the method is static
     * @param isFinal        whether the method is final
     * @param returnType     the return type of the method
     * @param parameterTypes the parameter types of the method
     * @param thrownTypes    the types of checked exceptions thrown by the method
     * @return the emitter for the method
     */
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
