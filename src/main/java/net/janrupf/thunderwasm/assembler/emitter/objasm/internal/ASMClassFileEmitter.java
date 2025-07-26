package net.janrupf.thunderwasm.assembler.emitter.objasm.internal;

import net.janrupf.thunderwasm.assembler.emitter.ClassFileEmitter;
import net.janrupf.thunderwasm.assembler.emitter.MethodEmitter;
import net.janrupf.thunderwasm.assembler.emitter.Visibility;
import net.janrupf.thunderwasm.assembler.emitter.signature.SignaturePart;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.util.ObjectUtil;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

public final class ASMClassFileEmitter implements ClassFileEmitter {
    private final ClassNode classNode;
    private final ObjectType owner;

    public ASMClassFileEmitter(
            String packageName,
            String className,
            ObjectType superType,
            List<ObjectType> interfaces
    ) {
        classNode = new ClassNode(Opcodes.ASM9);
        owner = new ObjectType(packageName, className);

        // Generate a Java 8 class
        classNode.visit(
                Opcodes.V1_8,
                Opcodes.ACC_PUBLIC,
                ASMConverter.convertType(owner).getInternalName(),
                null,
                ASMConverter.convertType(superType).getInternalName(),
                ASMConverter.convertTypesToNames(ObjectUtil.forceCast(interfaces))
        );
    }

    @Override
    public ObjectType getOwner() {
        return owner;
    }

    @Override
    public void field(
            String fieldName,
            Visibility visibility,
            boolean isStatic,
            boolean isFinal,
            JavaType type,
            SignaturePart signature
    ) {
        int access = ASMConverter.toAccessModifiers(visibility, isStatic, isFinal);
        Type asmType = ASMConverter.convertType(type);

        classNode.visitField(
                access,
                fieldName,
                asmType.getDescriptor(),
                ASMConverter.convertSignature(signature),
                null
        );
    }

    @Override
    public MethodEmitter method(
            String methodName,
            Visibility visibility,
            boolean isStatic,
            boolean isFinal,
            JavaType returnType,
            List<JavaType> parameterTypes,
            List<JavaType> thrownTypes
    ) {
        int access = ASMConverter.toAccessModifiers(visibility, isStatic, isFinal);
        Type asmReturnType = ASMConverter.convertType(returnType);
        Type[] asmParameterTypes = ASMConverter.convertTypes(parameterTypes);

        String descriptor = Type.getMethodDescriptor(asmReturnType, asmParameterTypes);

        MethodVisitor mVisitor = classNode.visitMethod(
                access,
                methodName,
                descriptor,
                null,
                ASMConverter.convertTypesToNames(thrownTypes)
        );

        return new ASMMethodEmitter(mVisitor, isStatic, owner, returnType, parameterTypes);
    }

    @Override
    public byte[] finish() {
        classNode.visitEnd();

        ClassWriter writer = new ClassWriter(0);
        classNode.accept(writer);

        return writer.toByteArray();
    }
}
