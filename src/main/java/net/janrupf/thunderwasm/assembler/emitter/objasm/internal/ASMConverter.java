package net.janrupf.thunderwasm.assembler.emitter.objasm.internal;

import net.janrupf.thunderwasm.assembler.emitter.Visibility;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

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
     * Convert an array of JavaTypes to an array of ASM descriptors.
     *
     * @param types the JavaTypes
     * @return the ASM descriptors
     */
    public static String[] convertTypesToDescriptors(JavaType[] types) {
        String[] result = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            result[i] = types[i].toJvmDescriptor();
        }
        return result;
    }
}
