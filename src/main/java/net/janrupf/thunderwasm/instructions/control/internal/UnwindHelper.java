package net.janrupf.thunderwasm.instructions.control.internal;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaStackFrameState;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;

public final class UnwindHelper {
    private UnwindHelper() {
        throw new AssertionError("Static helper class");
    }

    /**
     * Emit the code required for unwinding the stack.
     *
     * @param emitter      the emitter to use
     * @param valuesToKeep how many values to keep on top
     * @throws WasmAssemblerException if the code could not be emitted
     */
    public static void emitUnwindStack(
            CodeEmitter emitter,
            int valuesToKeep
    ) throws WasmAssemblerException {
        JavaStackFrameState frameState = emitter.getStackFrameState();
        int unwindCount = frameState.operandStackCount() - valuesToKeep;

        emitUnwindStack(emitter, valuesToKeep, unwindCount);
    }

    /**
     * Emit the code required for unwinding the stack partially.
     *
     * @param emitter      the emitter to use
     * @param valuesToKeep how many values to keep on top
     * @param unwindCount  how many values to discard from the stack
     * @throws WasmAssemblerException if the code could not be emitted
     */
    public static void emitUnwindStack(
            CodeEmitter emitter,
            int valuesToKeep,
            int unwindCount
    ) throws WasmAssemblerException {
        JavaStackFrameState frameState = emitter.getStackFrameState();

        if (unwindCount < 0) {
            throw new WasmAssemblerException("Stack underflow while emitting unwind");
        } else if (unwindCount == 0) {
            // Stack already in the correct state
            return;
        }

        if (valuesToKeep == 1 && unwindCount == 1) {
            JavaType topType = frameState.requireOperand(0);
            JavaType unwindType = frameState.requireOperand(1);

            if (topType.getSlotCount() < 2 && unwindType.getSlotCount() < 2) {
                // Simple swap and pop
                emitter.op(Op.SWAP);
                emitter.pop();
                return;
            }
        }

        // (maybe) slow path: Back up the values from the top of the stack into locals, discard, and push back
        JavaLocal[] locals = new JavaLocal[valuesToKeep];
        for (int i = 0; i < valuesToKeep; i++) {
            locals[i] = emitter.allocateLocal(frameState.requireOperand(i));
            emitter.storeLocal(locals[i]);
        }

        for (int i = 0; i < unwindCount; i++) {
            emitter.pop();
        }

        // Restore stack
        for (int i = locals.length - 1; i >= 0; i--) {
            emitter.loadLocal(locals[i]);
            locals[i].free();
        }
    }
}
