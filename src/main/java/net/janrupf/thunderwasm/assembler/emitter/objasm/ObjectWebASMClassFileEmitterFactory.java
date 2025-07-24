package net.janrupf.thunderwasm.assembler.emitter.objasm;

import net.janrupf.thunderwasm.assembler.emitter.ClassFileEmitter;
import net.janrupf.thunderwasm.assembler.emitter.ClassFileEmitterFactory;
import net.janrupf.thunderwasm.assembler.emitter.objasm.internal.ASMClassFileEmitter;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;

import java.util.List;

public class ObjectWebASMClassFileEmitterFactory implements ClassFileEmitterFactory {
    @Override
    public ClassFileEmitter createFor(
            String packageName,
            String className,
            ObjectType superType,
            List<ObjectType> interfaces
    ) {
        return new ASMClassFileEmitter(packageName, className, superType, interfaces);
    }
}
