package net.janrupf.thunderwasm.assembler.emitter.objasm.internal;

import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.MethodEmitter;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public final class ASMMethodEmitter implements MethodEmitter {
    private final MethodVisitor visitor;
    private final boolean isStatic;
    private final ObjectType owner;

    ASMMethodEmitter(MethodVisitor visitor, boolean isStatic, ObjectType owner) {
        this.visitor = visitor;
        this.isStatic = isStatic;
        this.owner = owner;
    }

    @Override
    public CodeEmitter code() {
        visitor.visitCode();
        return new ASMCodeEmitter(visitor, !isStatic, owner);
    }

    @Override
    public void finish() {
        visitor.visitEnd();
    }
}
