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
import net.janrupf.thunderwasm.instructions.decoder.InstructionDecoder;
import net.janrupf.thunderwasm.instructions.numeric.internal.PlainNumeric;
import net.janrupf.thunderwasm.runtime.WasmMath;
import net.janrupf.thunderwasm.types.NumberType;

public final class I32TruncSatF64U extends PlainNumeric {
    public static final I32TruncSatF64U INSTANCE = new I32TruncSatF64U();

    private I32TruncSatF64U() {
        super("i32.trunc_sat_f64_u", (byte) 0xFC);
    }

    @Override
    public InstructionDecoder getDecoder() {
        return InstructionDecoder.u32Variant(3, this);
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        frameState.popOperand(NumberType.F64);

        if (context.getConfiguration().strictNumericsEnabled()) {
            emitter.invoke(
                    ObjectType.of(WasmMath.class),
                    "strictI32TruncSatF64U",
                    new JavaType[]{PrimitiveType.DOUBLE},
                    PrimitiveType.INT,
                    InvokeType.STATIC,
                    false
            );
        } else {
            emitter.op(Op.D2L);
            emitter.op(Op.L2I);
        }

        frameState.pushOperand(NumberType.I32);
    }
}
