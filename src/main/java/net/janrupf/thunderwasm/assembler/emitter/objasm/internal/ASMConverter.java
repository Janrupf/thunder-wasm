package net.janrupf.thunderwasm.assembler.emitter.objasm.internal;

import net.janrupf.thunderwasm.assembler.emitter.Visibility;
import net.janrupf.thunderwasm.assembler.emitter.signature.ConcreteType;
import net.janrupf.thunderwasm.assembler.emitter.signature.SignaturePart;
import net.janrupf.thunderwasm.assembler.emitter.signature.TypeVariable;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

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
    public static Type[] convertTypes(JavaType[] types) {
        Type[] result = new Type[types.length];
        for (int i = 0; i < types.length; i++) {
            result[i] = convertType(types[i]);
        }
        return result;
    }

    /**
     * Convert an array of JavaTypes to an array of ASM names.
     *
     * @param types the JavaTypes
     * @return the ASM descriptors
     */
    public static String[] convertTypesToNames(JavaType[] types) {
        String[] result = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            result[i] = convertType(types[i]).getInternalName();
        }
        return result;
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
