package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.InvokeType;
import net.janrupf.thunderwasm.assembler.emitter.Op;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.types.NumberType;

public final class I64Rotr extends PlainNumeric {
    public static final I64Rotr INSTANCE = new I64Rotr();

    private I64Rotr() {
        super("i64.rotl", (byte) 0x8A);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        frameState.popOperand(NumberType.I64);
        frameState.requireOperand(NumberType.I64);
        emitter.op(Op.L2I);
        emitter.invoke(
                ObjectType.of(Long.class),
                "rotateRight",
                new JavaType[]{PrimitiveType.LONG, PrimitiveType.INT },
                PrimitiveType.LONG,
                InvokeType.STATIC,
                false
        );
    }
}
