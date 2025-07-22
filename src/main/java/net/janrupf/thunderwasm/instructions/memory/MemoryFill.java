package net.janrupf.thunderwasm.instructions.memory;


import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.instructions.WasmU32VariantInstruction;
import net.janrupf.thunderwasm.instructions.data.MemoryIndexData;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.NumberType;

import java.io.IOException;

public final class MemoryFill extends WasmU32VariantInstruction<MemoryIndexData> {
    public static final MemoryFill INSTANCE = new MemoryFill();

    public MemoryFill() {
        super("memory.fill", (byte) 0xFC, 11);
    }

    @Override
    public MemoryIndexData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return MemoryIndexData.read(loader);
    }

    @Override
    public void emitCode(CodeEmitContext context, MemoryIndexData data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I32);
        context.getFrameState().popOperand(NumberType.I32);
        context.getFrameState().popOperand(NumberType.I32);

        CodeEmitter emitter = context.getEmitter();
        MemoryInstructionHelper helper = new MemoryInstructionHelper(
                context.getLookups().requireMemory(data.toArrayIndex()),
                context
        );

        CodeLabel endLabel = emitter.newLabel();
        CodeLabel loopStartLabel = emitter.newLabel();

        JavaLocal nLocal = emitter.allocateLocal(PrimitiveType.INT);

        emitter.storeLocal(nLocal);
        emitter.loadLocal(nLocal);
        emitter.jump(JumpCondition.INT_EQUAL_ZERO, endLabel);

        emitter.resolveLabel(loopStartLabel);

        // Switch value and offset so value is on top, duplicate and store
        emitter.duplicate(2, 0);
        helper.emitMemoryStoreByte();

        // Increment d
        emitter.op(Op.SWAP);
        emitter.loadConstant(1);
        emitter.op(Op.IADD);
        emitter.op(Op.SWAP);

        // Decrement n and jump back to loop start if its greater than 0
        emitter.loadLocal(nLocal);
        emitter.loadConstant(1);
        emitter.op(Op.ISUB);
        emitter.duplicate();
        emitter.storeLocal(nLocal);
        emitter.jump(JumpCondition.INT_GREATER_THAN_ZERO, loopStartLabel);

        emitter.resolveLabel(endLabel);

        // Pop d and value
        emitter.pop();
        emitter.pop();

        nLocal.free();
    }
}
