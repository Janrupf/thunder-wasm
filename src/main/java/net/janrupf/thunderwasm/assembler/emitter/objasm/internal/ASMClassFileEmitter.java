package net.janrupf.thunderwasm.assembler.emitter.objasm.internal;

import net.janrupf.thunderwasm.assembler.emitter.ClassFileEmitter;
import net.janrupf.thunderwasm.assembler.emitter.MethodEmitter;
import net.janrupf.thunderwasm.assembler.emitter.Visibility;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class ASMClassFileEmitter implements ClassFileEmitter {
    private final ClassWriter writer;
    private final String fullyQualifiedName;

    public ASMClassFileEmitter(String packageName, String className) {
        writer = new ClassWriter(0);

        fullyQualifiedName = packageName.replace('.', '/') + '/' + className;

        // Generate a Java 8 class
        writer.visit(
                Opcodes.V1_8,
                Opcodes.ACC_PUBLIC,
                fullyQualifiedName,
                null,
                "java/lang/Object",
                new String[0]
        );
    }

    @Override
    public MethodEmitter method(
            String methodName,
            Visibility visibility,
            boolean isStatic,
            boolean isFinal,
            JavaType returnType,
            JavaType[] parameterTypes,
            JavaType[] thrownTypes
    ) {
        int access = ASMConverter.toAccessModifiers(visibility, isStatic, isFinal);
        Type asmReturnType = ASMConverter.convertType(returnType);
        Type[] asmParameterTypes = ASMConverter.convertTypes(parameterTypes);

        String descriptor = Type.getMethodDescriptor(asmReturnType, asmParameterTypes);

        MethodVisitor mVisitor = writer.visitMethod(
                access,
                methodName,
                descriptor,
                null,
                ASMConverter.convertTypesToDescriptors(thrownTypes)
        );

        return new ASMMethodEmitter(mVisitor, isStatic, Type.getType("L" + fullyQualifiedName + ";"));
    }

    @Override
    public byte[] finish() {
        writer.visitEnd();
        return writer.toByteArray();
    }
}
