package net.janrupf.thunderwasm.assembler.emitter.objasm.internal;

import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.MethodEmitter;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaStackFrameState;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public final class ASMMethodEmitter implements MethodEmitter {
    private final MethodVisitor visitor;
    private final boolean isStatic;
    private final ObjectType owner;
    private final JavaType returnType;
    private final JavaType[] argumentTypes;

    ASMMethodEmitter(MethodVisitor visitor, boolean isStatic, ObjectType owner, JavaType returnType, JavaType[] argumentTypes) {
        this.visitor = visitor;
        this.isStatic = isStatic;
        this.owner = owner;
        this.returnType = returnType;
        this.argumentTypes = argumentTypes;
    }

    @Override
    public CodeEmitter code() {
        visitor.visitCode();

        JavaStackFrameState stackFrameState = new JavaStackFrameState();

        JavaLocal thisLocal = null;
        if (!isStatic) {
            thisLocal = stackFrameState.allocateLocal(owner);
        }

        JavaLocal[] argumentLocals = new JavaLocal[argumentTypes.length];
        for (int i = 0; i < argumentLocals.length; i++) {
            argumentLocals[i] = stackFrameState.allocateLocal(argumentTypes[i]);
        }

        return new ASMCodeEmitter(visitor, owner, returnType, thisLocal, argumentLocals, stackFrameState);
    }

    @Override
    public void finish() {
        visitor.visitEnd();
    }
}
