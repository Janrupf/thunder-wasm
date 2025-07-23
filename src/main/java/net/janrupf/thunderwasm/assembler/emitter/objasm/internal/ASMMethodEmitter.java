package net.janrupf.thunderwasm.assembler.emitter.objasm.internal;

import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.MethodEmitter;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaStackFrameState;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.List;

public final class ASMMethodEmitter implements MethodEmitter {
    private final MethodVisitor visitor;
    private final ObjectType owner;
    private final JavaType returnType;
    private final JavaStackFrameState stackFrameState;

    private final JavaLocal thisLocal;
    private final List<JavaLocal> argumentLocals;

    ASMMethodEmitter(
            MethodVisitor visitor,
            boolean isStatic,
            ObjectType owner,
            JavaType returnType,
            List<JavaType> argumentTypes
    ) {
        this.visitor = visitor;
        this.owner = owner;
        this.returnType = returnType;
        this.stackFrameState = new JavaStackFrameState();

        if (!isStatic) {
            this.thisLocal = stackFrameState.allocateLocal(owner);
        } else {
            this.thisLocal = null;
        }

        this.argumentLocals = new ArrayList<>(argumentTypes.size());
        for (JavaType argumentType : argumentTypes) {
            argumentLocals.add(stackFrameState.allocateLocal(argumentType));
        }
    }

    @Override
    public JavaLocal getThisLocal() {
        return thisLocal;
    }

    @Override
    public List<JavaLocal> getArgumentLocals() {
        return argumentLocals;
    }

    @Override
    public CodeEmitter code() {
        visitor.visitCode();
        return new ASMCodeEmitter(visitor, owner, returnType, stackFrameState);
    }

    @Override
    public void finish() {
        visitor.visitEnd();
    }
}
