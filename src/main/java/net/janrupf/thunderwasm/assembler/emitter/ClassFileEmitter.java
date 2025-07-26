package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.emitter.signature.SignaturePart;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;

import java.util.List;

/**
 * Abstraction layer for replacing the underlying code generation
 * framework.
 * <p>
 * Note that this is <b>not a strict visitor pattern</b>. This means
 * it is valid to call {@link #field} or any other member except
 * {@link #finish} while a method is not finalized yet, including
 * nesting calls to {@link #method}.
 */
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
     * @param signature  the generic signature of the field
     */
    void field(
            String fieldName,
            Visibility visibility,
            boolean isStatic,
            boolean isFinal,
            JavaType type,
            SignaturePart signature
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
            List<JavaType> parameterTypes,
            List<JavaType> thrownTypes
    );

    /**
     * Finalizes the class file and returns the bytecode.
     *
     * @return the bytecode of the class file
     */
    byte[] finish();
}
