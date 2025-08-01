package net.janrupf.thunderwasm.test.assembler.continuation;

import net.janrupf.thunderwasm.ThunderWasmException;
import net.janrupf.thunderwasm.runtime.continuation.Continuation;
import net.janrupf.thunderwasm.runtime.continuation.ContinuationLayer;
import net.janrupf.thunderwasm.runtime.linker.RuntimeLinker;
import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;
import net.janrupf.thunderwasm.runtime.linker.memory.LinkedMemory;
import net.janrupf.thunderwasm.runtime.state.MultiValue;
import net.janrupf.thunderwasm.types.FunctionType;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;

public class ContinuationLinker implements RuntimeLinker {
    private final LinkedFunction suspender;
    private final LinkedFunction log;

    private LinkedMemory memory;

    public ContinuationLinker() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        this.suspender = LinkedFunction.Simple.inferFromMethodHandle(
                lookup.findVirtual(ContinuationLinker.class, "suspender", MethodType.methodType(
                        int.class,
                        int.class,
                        Continuation.class
                )).bindTo(this)
        );

        this.log = LinkedFunction.Simple.inferFromMethodHandle(
                lookup.findVirtual(ContinuationLinker.class, "log", MethodType.methodType(void.class, int.class))
                        .bindTo(this)
        );
    }

    public void setMemory(LinkedMemory memory) {
        this.memory = memory;
    }

    // Function that always suspends on the first call and always resumes when called with
    // a continuation that was suspended.
    private int suspender(int arg, Continuation continuation) {
        ContinuationLayer layer = continuation.popLayer();
        if (layer == null) {
            // Unsuspended invocation
            ((TestContinuationCallback) continuation.getCallback()).suspendNow();

            MultiValue stateSave = MultiValue.allocate(1, 0, 0, 0, 0);
            stateSave.putInt(arg);

            layer = ContinuationLayer.create(null, 0, null, stateSave);
            continuation.pushLayer(layer);
            return -1;
        }

        // Suspended invocation
        if (layer.getPointId() != 0) {
            throw new IllegalStateException("Only supported continuation point is point 0");
        }

        arg = layer.getLocals().popInt();
        return arg * 4;
    }

    private void log(int messagePtr) {
        ByteBuffer memoryBuffer = this.memory.asInternal();

        memoryBuffer.position(messagePtr);

        byte[] buffer = new byte[1024];
        int index = 0;
        while (index < buffer.length) {
            byte c = memoryBuffer.get();
            if (c == 0) {
                break;
            }

            buffer[index++] = c;
        }

        String str = new String(buffer, 0, index);
        System.out.println("[WASM] " + str);
    }

    @Override
    public LinkedFunction linkFunction(String moduleName, String importName, FunctionType type) throws ThunderWasmException {
        if (moduleName.equals("continuations")) {
            switch (importName) {
                case "suspender":
                    return suspender;

                case "log":
                    return log;
            }
        }

        throw new ThunderWasmException("Unknown import: " + moduleName + "#" + importName);
    }
}
