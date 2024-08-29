package net.janrupf.thunderwasm.assembler.emitter.objasm;

import net.janrupf.thunderwasm.assembler.emitter.ClassFileEmitter;
import net.janrupf.thunderwasm.assembler.emitter.ClassFileEmitterFactory;
import net.janrupf.thunderwasm.assembler.emitter.objasm.internal.ASMClassFileEmitter;

public class ObjectWebASMClassFileEmitterFactory implements ClassFileEmitterFactory {
    @Override
    public ClassFileEmitter createFor(String packageName, String className) {
        return new ASMClassFileEmitter(packageName, className);
    }
}
