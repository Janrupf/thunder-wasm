package net.janrupf.thunderwasm.assembler.emitter.objasm.internal;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.InvokeType;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.assembler.emitter.Visibility;
import net.janrupf.thunderwasm.assembler.emitter.signature.ConcreteType;
import net.janrupf.thunderwasm.assembler.emitter.signature.SignaturePart;
import net.janrupf.thunderwasm.assembler.emitter.signature.TypeVariable;
import net.janrupf.thunderwasm.assembler.emitter.types.*;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ASMConverter {
    private ASMConverter() {
        throw new AssertionError("No instances of ASMConverter are allowed");
    }

    /**
     * Convert from friendly ThunderWASM visibility to ASM visibility.
     *
     * @param visibility the visibility
     * @param isStatic   whether the member is static
     * @param isFinal    whether the member is final
     * @return the ASM access modifiers
     */
    public static int toAccessModifiers(
            Visibility visibility,
            boolean isStatic,
            boolean isFinal
    ) {
        int access = 0;

        switch (visibility) {
            case PUBLIC:
                access |= Opcodes.ACC_PUBLIC;
                break;
            case PROTECTED:
                access |= Opcodes.ACC_PROTECTED;
                break;
            case PACKAGE_PRIVATE:
                break;
            case PRIVATE:
                access |= Opcodes.ACC_PRIVATE;
                break;
            default:
                throw new IllegalArgumentException("Unknown visibility: " + visibility);
        }

        if (isStatic) {
            access |= Opcodes.ACC_STATIC;
        }

        if (isFinal) {
            access |= Opcodes.ACC_FINAL;
        }

        return access;
    }

    /**
     * Convert a JavaType to an ASM Type.
     *
     * @param t the JavaType
     * @return the ASM Type
     */
    public static Type convertType(JavaType t) {
        if (t == null) {
            return null;
        }

        return Type.getType(t.toJvmDescriptor());
    }

    /**
     * Convert an array of JavaTypes to an array of ASM Types.
     *
     * @param types the JavaTypes
     * @return the ASM Types
     */
    public static Type[] convertTypes(List<JavaType> types) {
        Type[] result = new Type[types.size()];
        for (int i = 0; i < types.size(); i++) {
            result[i] = convertType(types.get(i));
        }
        return result;
    }

    /**
     * Convert an array of JavaTypes to an array of ASM names.
     *
     * @param types the JavaTypes
     * @return the ASM descriptors
     */
    public static String[] convertTypesToNames(List<JavaType> types) {
        String[] result = new String[types.size()];
        for (int i = 0; i < types.size(); i++) {
            result[i] = convertType(types.get(i)).getInternalName();
        }
        return result;
    }

    /**
     * Convert a method signature to an ASM Type.
     *
     * @param parameterTypes the parameter types of the method
     * @param returnType     the return type of the method
     * @return the ASM Type representing the method signature
     */
    public static Type convertMethod(List<JavaType> parameterTypes, JavaType returnType) {
        return Type.getMethodType(convertType(returnType), convertTypes(parameterTypes));
    }

    /**
     * Convert a method handle to an ASM Handle.
     *
     * @param handle the JavaMethodHandle to convert
     * @return the ASM Handle representing the method handle
     * @throws WasmAssemblerException if the invoke type is unknown or invalid
     */
    public static Handle convertMethodHandle(JavaMethodHandle handle) throws WasmAssemblerException {
        return convertMethodHandle(
                handle.getOwner(),
                handle.getName(),
                handle.getParameterTypes(),
                handle.getReturnType(),
                handle.getInvokeType(),
                handle.isOwnerIsInterface()
        );
    }

    /**
     * Convert a method handle to an ASM Handle.
     *
     * @param type             the type of the method handle, typically an ObjectType
     * @param methodName       the name of the method
     * @param parameterTypes   the parameter types of the method
     * @param returnType       the return type of the method
     * @param invokeType       the type of the invocation (e.g., STATIC, VIRTUAL, etc.)
     * @param ownerIsInterface whether the owner of the method is an interface
     * @return the ASM Handle representing the method handle
     * @throws WasmAssemblerException if the invoke type is unknown or invalid
     */
    public static Handle convertMethodHandle(
            JavaType type,
            String methodName,
            List<JavaType> parameterTypes,
            JavaType returnType,
            InvokeType invokeType,
            boolean ownerIsInterface
    ) throws WasmAssemblerException {
        int tag;
        switch (invokeType) {
            case INTERFACE:
                tag = Opcodes.H_INVOKEINTERFACE;
                break;
            case SPECIAL:
                tag = Opcodes.H_INVOKESPECIAL;
                break;
            case STATIC:
                tag = Opcodes.H_INVOKESTATIC;
                break;
            case VIRTUAL:
                tag = Opcodes.H_INVOKEVIRTUAL;
                break;
            default:
                throw new WasmAssemblerException("Unknown invoke type: " + invokeType);
        }

        String owner = convertType(type).getInternalName();
        String methodDescriptor = convertMethod(parameterTypes, returnType).getDescriptor();

        return new Handle(
                tag,
                owner,
                methodName,
                methodDescriptor,
                ownerIsInterface
        );
    }

    /**
     * Convert a field handle to an ASM handle.
     *
     * @param handle the handle to convert
     * @return the converted handle
     */
    public static Handle convertFieldHandle(JavaFieldHandle handle) {
        return convertFieldHandle(
                handle.getOwner(),
                handle.getName(),
                handle.getType(),
                handle.isStatic(),
                handle.isSet(),
                handle.isOwnerInterface()
        );
    }

    /**
     * Convert a field handle to an ASM handle.
     *
     * @param owner            the type owning the field
     * @param name             the name of the field
     * @param type             the type of the field
     * @param isStatic         whether the field is static
     * @param isSet            whether the handle is a setter
     * @param ownerIsInterface whether the field owner is an interface
     * @return the converted handle
     */
    private static Handle convertFieldHandle(
            JavaType owner,
            String name,
            JavaType type,
            boolean isStatic,
            boolean isSet,
            boolean ownerIsInterface
    ) {
        int tag;
        if (isStatic && !isSet) {
            tag = Opcodes.H_GETSTATIC;
        } else if (!isStatic && !isSet) {
            tag = Opcodes.H_GETFIELD;
        } else if (isStatic) {
            tag = Opcodes.H_PUTSTATIC;
        } else {
            tag = Opcodes.H_PUTFIELD;
        }

        String ownerName = convertType(owner).getInternalName();

        return new Handle(
                tag,
                ownerName,
                name,
                convertType(type).getDescriptor(),
                ownerIsInterface
        );
    }

    /**
     * Convert a signature part to a signature string.
     *
     * @param part the signature part
     * @return the signature string
     */
    public static String convertSignature(SignaturePart part) {
        if (part == null) {
            return null;
        }

        SignatureWriter writer = new SignatureWriter();
        convertSignature(writer, part);
        writer.visitEnd();
        return writer.toString();
    }

    private static void convertSignature(SignatureVisitor visitor, SignaturePart part) {
        if (part == null) {
            return;
        }

        if (part instanceof ConcreteType) {
            ConcreteType concreteType = (ConcreteType) part;
            visitor.visitClassType(convertType(concreteType.getType()).getInternalName());
            if (!concreteType.getTypeArguments().isEmpty()) {
                for (SignaturePart typeArgument : concreteType.getTypeArguments()) {
                    SignatureVisitor nested = visitor.visitTypeArgument('=');
                    convertSignature(nested, typeArgument);
                    nested.visitEnd();
                }
            }
        } else if (part instanceof TypeVariable) {
            TypeVariable typeVariable = (TypeVariable) part;
            visitor.visitTypeVariable(typeVariable.getName());
        } else {
            throw new IllegalArgumentException("Unknown signature part: " + part);
        }
    }
}
